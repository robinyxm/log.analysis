package etl.cmd.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
//log4j2
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import etl.cmd.SftpCmd;
import etl.engine.EngineUtil;
import etl.util.HdfsUtil;
import etl.util.SftpUtil;
import etl.util.StringUtil;
import serp.util.Strings;

public class TestSftpCmd extends TestETLCmd {
	public static final Logger logger = LogManager.getLogger(TestSftpCmd.class);

	@Before
	public void beforeMethod() {
		org.junit.Assume.assumeTrue(super.isTestSftp());
	}
	
	public String getResourceSubFolder(){
		return "sftp"+File.separator;
	}
	
	@Test
	public void test1() throws Exception {
		String dfsCfg = "/test/sftpcmd/cfg/";
		String cfg = "sftp_test.properties";
		// values in the static cfg
		String fileName = "backup_test1_data";
		
		getFs().copyFromLocalFile(false, true, new Path(getLocalFolder() + cfg), new Path(dfsCfg + cfg));
		SftpCmd cmd = new SftpCmd("wf1", null, dfsCfg + cfg, getDefaultFS(), null);
		String ftpFolder = cmd.getFromDirs()[0];
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		FileUtils.deleteDirectory(new File(ftpFolder));
		FileUtils.copyDirectory(new File(super.getLocalFolder()+"data"), new File(ftpFolder));
		
		cmd.sgProcess();
		// check incoming fodler
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		assertTrue(fl.contains(fileName));
		// check remote dir
		String sftpHost = EngineUtil.getInstance().getEngineProp().getString("sftp.host");
		String sftpUser = EngineUtil.getInstance().getEngineProp().getString("sftp.user");
		int sftpPort = EngineUtil.getInstance().getEngineProp().getInt("sftp.port");
		String sftpPass = EngineUtil.getInstance().getEngineProp().getString("sftp.pass");
		fl = SftpUtil.sftpList(sftpHost, sftpPort, sftpUser, sftpPass, ftpFolder);
		assertFalse(fl.contains(fileName));
	}

	public void testSftpSessionConnectionFailure() throws Exception {
		logger.info("Testing sftpcmd common failure test case");
		String fileName = "backup_test1_data";
		String dfsFolder = "/test/sftpcmd/cfg/";
		String cfg = "sftp_test.properties";
		
		getFs().copyFromLocalFile(false, true, new Path(getLocalFolder() + cfg), new Path(dfsFolder + cfg));
		SftpCmd cmd = new SftpCmd("wf1", null, dfsFolder + cfg, getDefaultFS(), null);
		cmd.setHost("192.168.12.13");
		cmd.setSftpConnectRetryWait(5*1000);
		cmd.setSftpConnectRetryCount(2);
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		cmd.sgProcess();
		// check incoming fodler
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		assertFalse(fl.contains(fileName));
	}
	
	@Test
	public void testDeleteFileNotEnabledFun() throws Exception{
		logger.info("Testing sftpcmd delete not enabled test case");

		String dfsCfg = "/test/sftpcmd/cfg/";
		String cfg = "sftp_test.properties";
		// values in the static cfg
		String fileName = "backup_test1_data";
		
		getFs().copyFromLocalFile(false, true, new Path(getLocalFolder() + cfg), new Path(dfsCfg + cfg));
		SftpCmd cmd = new SftpCmd("wf1", null, dfsCfg + cfg, getDefaultFS(), null);
		String ftpFolder = cmd.getFromDirs()[0];
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		
		FileUtils.deleteDirectory(new File(ftpFolder));
		FileUtils.copyDirectory(new File(super.getLocalFolder()+"data"), new File(ftpFolder));
		cmd.setSftpClean(false);
		cmd.sgProcess();
		//
		// check incoming folder
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		assertTrue(fl.contains(fileName));
		// check remote dir
		String sftpHost = EngineUtil.getInstance().getEngineProp().getString("sftp.host");
		String sftpUser = EngineUtil.getInstance().getEngineProp().getString("sftp.user");
		int sftpPort = EngineUtil.getInstance().getEngineProp().getInt("sftp.port");
		String sftpPass = EngineUtil.getInstance().getEngineProp().getString("sftp.pass");
		fl = SftpUtil.sftpList(sftpHost, sftpPort, sftpUser, sftpPass, ftpFolder);
		assertTrue(fl.contains(fileName));
	}
	
	@Test
	public void testFileFilter() throws Exception {
		String dfsCfg = "/test/sftpcmd/cfg/";
		String cfg = "sftp_selectfile.properties";
		
		getFs().copyFromLocalFile(false, true, new Path(getLocalFolder() + cfg), new Path(dfsCfg + cfg));
		SftpCmd cmd = new SftpCmd("wf1", null, dfsCfg + cfg, getDefaultFS(), null);
		String ftpFolder = cmd.getFromDirs()[0];
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		FileUtils.deleteDirectory(new File(ftpFolder));
		FileUtils.copyDirectory(new File(super.getLocalFolder()+"data"), new File(ftpFolder));
		cmd.sgProcess();
		
		// check incoming fodler
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		assertFalse(fl.contains("RTDB_ACCESS.friday"));
		assertTrue(fl.contains("RTDB_ACCESS.monday"));
	}
	
	@Test
	public void testMultiFolders() throws Exception {
		String dfsCfg = "/test/sftpcmd/cfg/";
		String cfg = "sftp_multiple_dirs.properties";
		String fileName0 = "RTDB_ACCESS.friday";
		String fileName1 = "RTDB_ACCESS.monday";
		
		getFs().copyFromLocalFile(false, true, new Path(getLocalFolder() + cfg), new Path(dfsCfg + cfg));
		SftpCmd cmd = new SftpCmd("wf1", null, dfsCfg + cfg, getDefaultFS(), null);
		String ftpFolder = StringUtil.commonPath(cmd.getFromDirs());
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		FileUtils.deleteDirectory(new File(ftpFolder));
		FileUtils.copyDirectory(new File(super.getLocalFolder()+"data"), new File(ftpFolder));
		List<String> ret = cmd.sgProcess();
		logger.info(ret);
		
		//assertion
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		logger.info(fl);
		assertTrue(fl.contains(fileName0));
		assertTrue(fl.contains(fileName1));
	}
	
	@Test
	public void testLimitFiles() throws Exception {
		String dfsCfg = "/test/sftpcmd/cfg/";
		String cfg = "sftp_limit.properties";
		
		getFs().copyFromLocalFile(false, true, new Path(getLocalFolder() + cfg), new Path(dfsCfg + cfg));
		SftpCmd cmd = new SftpCmd("wf1", null, dfsCfg + cfg, getDefaultFS(), null);
		
		String dfsIncomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(dfsIncomingFolder), true);
		getFs().mkdirs(new Path(dfsIncomingFolder));
		String ftpFolder = cmd.getFromDirs()[0];
		FileUtils.deleteDirectory(new File(ftpFolder));
		FileUtils.copyDirectory(new File(super.getLocalFolder()+"data"), new File(ftpFolder));
		
		List<String> ret = cmd.sgProcess();
		logger.info(ret);
		
		//assertion
		List<String> fl = HdfsUtil.listDfsFile(getFs(), dfsIncomingFolder);
		logger.info(fl);
		assertTrue(fl.size()==1);
	}
	
	//you need to manually mkdir cmd.getFromDirs
	@Test
	public void fileNamesOnly() throws Exception {
		if (!super.isTestSftp()) return;
		String dfsCfg = "/test/sftpcmd/cfg/";
		String cfg = "sftp_filenames.properties";
		
		getFs().copyFromLocalFile(false, true, new Path(getLocalFolder() + cfg), new Path(dfsCfg + cfg));
		SftpCmd cmd = new SftpCmd("wf1", null, dfsCfg + cfg, getDefaultFS(), null);
		
		String dfsIncomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(dfsIncomingFolder), true);
		getFs().mkdirs(new Path(dfsIncomingFolder));
		String ftpFolder = cmd.getFromDirs()[0];
		FileUtils.deleteDirectory(new File(ftpFolder));
		FileUtils.copyDirectory(new File(super.getLocalFolder()+"data"), new File(ftpFolder));
		List<String> ret = cmd.sgProcess();
		logger.info(ret);
		
		//assertion
		List<String> fl = HdfsUtil.listDfsFile(cmd.getFs(), dfsIncomingFolder);
		String file = fl.get(0);//file name is map key
		logger.info(fl);
		assertTrue(fl.size()==1);
		assertTrue(fl.get(0).equals("0"));
		
		List<String> contents = HdfsUtil.stringsFromDfsFile(cmd.getFs(), dfsIncomingFolder+file);
		logger.info(Strings.join(contents.toArray(new String[]{}),"\n"));
		assertTrue(contents.size()==3);
	}
}