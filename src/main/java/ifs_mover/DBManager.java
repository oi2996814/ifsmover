/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* ifsmover is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* ifsmover 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* ifsmover 개발팀은 사전 공지, 허락, 동의 없이 ifsmover 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/

package ifs_mover;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

public class DBManager {
	private static final Logger logger = LoggerFactory.getLogger(DBManager.class);
	
	private static Connection con;
	private static final String NAS = "nas";
	private static final String JDBC = "org.sqlite.JDBC";
	private static final String DB_FILE_URL = "jdbc:sqlite:ifs-mover.db";
	
	private static final int JOB_ERROR = 10;
	private static final int CACHE_SIZE = 10000;
	private static final int WAIT_TIMEOUT = 20000;
	
	private static final String CREATE_JOB_TABLE =
			"CREATE TABLE IF NOT EXISTS 'JOB' (\n"
			+ "'job_id' INTEGER NOT NULL,\n"
			+ "'job_state' INTEGER NOT NULL DEFAULT 0,\n"
			+ "'pid' INTEGER NOT NULL,\n"
			+ "'job_type' TEXT NOT NULL,\n"
			+ "'source_point' TEXT NOT NULL,\n"
			+ "'target_point' TEXT NOT NULL, \n"
			+ "'objects_count' INTEGER DEFAULT 0,\n"
			+ "'objects_size' INTEGER DEFAULT 0,\n"
			+ "'moved_objects_count' INTEGER DEFAULT 0,\n"
			+ "'moved_objects_size' INTEGER DEFAULT 0,\n"
			+ "'failed_count' INTEGER DEFAULT 0,\n"
			+ "'failed_size' INTEGER DEFAULT 0,\n"
			+ "'skip_objects_count' INTEGER DEFAULT 0,\n"
			+ "'skip_objects_size' INTEGER DEFAULT 0,\n"
			+ "'start' TEXT,\n"
			+ "'end' TEXT,\n"
			+ "'error_desc' TEXT,\n"
			+ "PRIMARY KEY('job_id' AUTOINCREMENT));";
	
	private static final String UPDATE_JOB_ID = "UPDATE JOB_";
	private static final String INSERT_JOB_ID = "INSERT INTO JOB_";
	private static final String SQL_UPDATE_JOB_STATE_MOVE = "UPDATE JOB SET job_state = 1 WHERE job_id = ?";
	private static final String SQL_UPDATE_JOB_STATE_COMPLETE = "UPDATE JOB SET job_state = 4 WHERE job_id = ?";
	private static final String SQL_UPDATE_JOB_STATE_STOP = "UPDATE JOB SET job_state = 5 WHERE job_id = ?";
	private static final String SQL_UPDATE_JOB_STATE_REMOVE = "UPDATE JOB SET job_state = 6 WHERE job_id = ?";
	private static final String SQL_UPDATE_JOB_STATE_RERUN = "UPDATE JOB SET job_state = 7 WHERE job_id = ?";
	private static final String SQL_UPDATE_JOB_STATE_RERUN_MOVE = "UPDATE JOB SET job_state = 8 WHERE job_id = ?";
	private static final String SQL_SELECT_JOB_STATUS = "SELECT job_id, job_state, job_type, source_point, target_point, objects_count, objects_size, moved_objects_count, moved_objects_size, failed_count, failed_size, skip_objects_count, skip_objects_size, start, end, error_desc FROM JOB ORDER BY job_id";
	
	private static final String SQL_UPDATE_JOB_OBJECTS = "UPDATE JOB SET objects_count = objects_count + 1, objects_size = objects_size + ? WHERE job_id =  ?";
	private static final String SQL_UPDATE_JOB_FAILED_OBJECTS = "UPDATE JOB SET failed_count = failed_count + 1, failed_size = failed_size + ? WHERE job_id =  ?";
	private static final String SQL_UPDATE_JOB_RERUN_SKIP = "UPDATE JOB SET objects_count = objects_count + 1, objects_size = objects_size + ?, skip_objects_count = skip_objects_count + 1, skip_objects_size = skip_objects_size + ? WHERE job_id = ?";
	private static final String SQL_UPDATE_JOB_RERUN_OBJECTS = "UPDATE JOB SET objects_count = objects_count + 1, objects_size = objects_size + ? WHERE job_id =  ?";
	private static final String SQL_UPDATE_JOB_MOVED = "UPDATE JOB SET moved_objects_count = moved_objects_count + 1, moved_objects_size = moved_objects_size + ? WHERE job_id = ?";
	private static final String SQL_UPDATE_JOB_MOVED_COUNT = "UPDATE JOB SET moved_objects_count = moved_objects_count + ?, moved_objects_size = moved_objects_size + ? WHERE job_id = ?";
	private static final String SQL_UPDATE_JOB_ERROR = "UPDATE JOB SET job_state = ?, error_desc = ? WHERE job_id = ";
	private static final String SQL_INSERT_JOB = "INSERT INTO JOB(pid, job_type, source_point, target_point, start) VALUES(?, ?, ?, ?, datetime('now', 'localtime'))";
	private static final String SQL_UPDATE_JOB_START = "UPDATE JOB SET start = datetime('now', 'localtime') WHERE job_id =";
	private static final String SQL_UPDATE_JOB_END = "UPDATE JOB SET end = datetime('now', 'localtime') WHERE job_id =";
	
	private static final String SQL_OBJECT_WHERE_PATH = "_OBJECTS WHERE path = '";
	private static final String SQL_SKIP_CHECK = "_OBJECTS SET object_state = 1, skip_check = 1, mtime = '";
	private static final String SQL_SKIP_CHECK_WHERE_PATH = "_OBJECTS SET skip_check = 1 WHERE path = '";
	private static final String SQL_SIZE = "', size = ";
	private static final String SQL_WHERE_PATH = " WHERE path = '";
	private static final String SQL_VERSIONID_IS_NULL = "' and version_id is null";
	private static final String SQL_VERSIONID = "' and version_id = '";

	private DBManager() {
		// 
	}
	
	public static void init() {
		try {
			Class.forName(JDBC);
			SQLiteConfig config = new SQLiteConfig();
			config.setCacheSize(CACHE_SIZE);
			config.setBusyTimeout(WAIT_TIMEOUT);
			config.setTransactionMode(SQLiteConfig.TransactionMode.DEFERRED);
    		config.setLockingMode(SQLiteConfig.LockingMode.NORMAL);
    		config.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
    		config.setJournalMode(SQLiteConfig.JournalMode.WAL);
			config.setEncoding(SQLiteConfig.Encoding.UTF_8);
			DBManager.con = DriverManager.getConnection(DB_FILE_URL, config.toProperties());
		} catch (SQLException | ClassNotFoundException e) {
			logger.error(e.getMessage());
		}
	}
	
	private static Connection getReadConnection() {
		Connection con = null;
		try {
			Class.forName(JDBC);
			SQLiteConfig config = new SQLiteConfig();
			config.setReadOnly(true);
			con = DriverManager.getConnection(DB_FILE_URL, config.toProperties());
		} catch (SQLException | ClassNotFoundException e) {
			logger.error(e.getMessage());
		}

		return con;
	}

	public static void open() {
		if (DBManager.con == null) {
			try {
				Class.forName(JDBC);
				DBManager.con = DriverManager.getConnection(DB_FILE_URL);
			} catch (SQLException | ClassNotFoundException e) {
				logger.error(e.getMessage());
			}
		}
	}

	public static void close() {
		if (DBManager.con != null) {
			try {
				DBManager.con.close();
			} catch (SQLException e) {
				logger.error(e.getMessage());
			}
			DBManager.con = null;
		}
	}
	
	public static Connection getConnection() {
		open();
		return DBManager.con;
	}
	

	
	public static void createJobTable() {
		open();
		try (Statement stmt = con.createStatement()) {
			stmt.execute(CREATE_JOB_TABLE);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static void createJob(String pid, String select, Config sourceConfig, Config targetConfig) {
		open();
		try(PreparedStatement pstmt = con.prepareStatement(SQL_INSERT_JOB);) {
			pstmt.setInt(1, Integer.parseInt(pid));
			pstmt.setString(2, select);
			if (NAS.compareToIgnoreCase(select) == 0) {
				pstmt.setString(3, sourceConfig.getMountPoint() + sourceConfig.getPrefix());
			} else {
				pstmt.setString(3, sourceConfig.getBucket());
			}
			pstmt.setString(4, targetConfig.getBucket());
			
			pstmt.executeUpdate();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static String getJobId(String pid) {
		String jobId = "";
		open();
		final String sql = "SELECT job_id FROM JOB WHERE pid = " + pid;
		try (Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(sql);) {
			if (rs.next()) {
				jobId = rs.getString(1);
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		return jobId;
	}
	
	public static boolean updateJobInfo(String jobId, long size) {
		open();
		try(PreparedStatement pstmt = con.prepareStatement(SQL_UPDATE_JOB_OBJECTS)) {
			pstmt.setLong(1, size);
			pstmt.setString(2, jobId);
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}
	
	public static boolean updateJobFailedInfo(String jobId, long size) {
		open();
		try(PreparedStatement pstmt = con.prepareStatement(SQL_UPDATE_JOB_FAILED_OBJECTS)) {
			pstmt.setLong(1, size);
			pstmt.setString(2, jobId);
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}
	
	public static boolean updateJobRerunInfo(String jobId, long size) {
		open();
		try(PreparedStatement pstmt = con.prepareStatement(SQL_UPDATE_JOB_RERUN_OBJECTS)) {
			pstmt.setLong(1, size);
			pstmt.setString(2, jobId);
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}
	
	public static boolean updateJobRerunSkipInfo(String jobId, long size) {
		open();
		try(PreparedStatement pstmt = con.prepareStatement(SQL_UPDATE_JOB_RERUN_SKIP)) {
			pstmt.setLong(1, size);
			pstmt.setLong(2, size);
			pstmt.setString(3, jobId);
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}
	
	public static void createMoveObjectTable(String jobId) {
		open();
		String sql = "CREATE TABLE IF NOT EXISTS JOB_" + jobId + "_OBJECTS (\n"
				+ "'sequence' INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "'path' TEXT NOT NULL,\n"
				+ "'size' INTEGER NOT NULL,\n"
				+ "'object_state' INTEGER NOT NULL DEFAULT 0,\n"
				+ "'isfile'	INTEGER NOT NULL,\n"
				+ "'skip_check' INTEGER DEFAULT 0,\n"
				+ "'mtime' TEXT NOT NULL,\n"
				+ "'version_id' TEXT,\n"
				+ "'isdelete' INTEGER DEFAULT 0,\n"
				+ "'islatest' INTEGER DEFAULT 0,\n"
				+ "'error_date' TEXT,\n"
				+ "'error_code' TEXT,\n"
				+ "'error_desc' TEXT,\n"
				+ "UNIQUE('sequence', 'path'))";
		try(Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static void createMoveObjectTableIndex(String jobId) {
		open();
		String sql = "CREATE INDEX PATH_" + jobId + "_INDEX ON JOB_" + jobId + "_OBJECTS (path)";
		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static boolean insertMoveObject(String jobId, boolean isFile, String mTime, long size, String path) {
		open();
		String sql = INSERT_JOB_ID + jobId + "_OBJECTS (path, object_state, isfile, mtime, size) VALUES(?, 1, ?, ?, ?)";
		try (PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setString(1, path);
			if (isFile) {
				pstmt.setInt(2, 1);
			} else {
				pstmt.setInt(2, 0);
			}
			pstmt.setString(3, mTime);
			pstmt.setLong(4, size);
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}

	public static boolean insertMoveObjectVersioning(String jobId, boolean isFile, String mTime, long size, String path, String versionId, boolean isDelete, boolean isLatest) {
		open();
		String sql = INSERT_JOB_ID + jobId + "_OBJECTS (path, object_state, isfile, mtime, size, version_id, isdelete, islatest) VALUES(?, 1, ?, ?, ?, ?, ?, ?)";
		try  (PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setString(1, path);
			if (isFile) {
				pstmt.setInt(2, 1);
			} else {
				pstmt.setInt(2, 0);
			}
			pstmt.setString(3, mTime);
			pstmt.setLong(4, size);
			if (versionId == null || versionId.isEmpty() || versionId.compareTo("null") == 0) {
				pstmt.setNull(5, java.sql.Types.NULL);
			} else {
				pstmt.setString(5, versionId);
			}
			if (isDelete) {
				pstmt.setInt(6, 1);
			} else {
				pstmt.setInt(6, 0);
			}
			if (isLatest) {
				pstmt.setInt(7, 1);
			} else {
				pstmt.setInt(7, 0);
			}
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}

	
	public static boolean updateToMoveObject(String jobId, String mTime, long size, String path) {
		open();
		String sql = UPDATE_JOB_ID + jobId + SQL_SKIP_CHECK + mTime + SQL_SIZE + size + SQL_WHERE_PATH + path + "'";
		try (Statement stmt = con.createStatement()) {
			if (stmt.executeUpdate(sql) == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}

	public static boolean updateToMoveObjectVersion(String jobId, String mTime, long size, String path, String versionId) {
		open();
		String sql;
		if (versionId == null || versionId.isEmpty() || versionId.compareTo("null") == 0) { 
			sql = UPDATE_JOB_ID + jobId + SQL_SKIP_CHECK + mTime + SQL_SIZE + size + SQL_WHERE_PATH + path + SQL_VERSIONID_IS_NULL;
		} else {
			sql = UPDATE_JOB_ID + jobId + SQL_SKIP_CHECK + mTime + SQL_SIZE + size + SQL_WHERE_PATH + path + SQL_VERSIONID + versionId + "'";
		}

		try (Statement stmt = con.createStatement()) {
			if (stmt.executeUpdate(sql) == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}
	
	public static boolean insertRerunMoveObject(String jobId, boolean isFile, String mTime, long size, String path) {
		open();
		String sql = INSERT_JOB_ID + jobId + "_OBJECTS (path, object_state, skip_check, isfile, mtime, size) VALUES(?, 1, 1, ?, ?, ?)";
		try(PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setString(1, path);
			if (isFile) {
				pstmt.setInt(2, 1);
			} else {
				pstmt.setInt(2, 0);
			}
			pstmt.setString(3, mTime);
			pstmt.setLong(4, size);
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}

	public static boolean insertRerunMoveObjectVersion(String jobId, boolean isFile, String mTime, long size, String path, String versionId, boolean isDelete, boolean isLatest) {
		open();
		String sql = INSERT_JOB_ID + jobId + "_OBJECTS (path, object_state, skip_check, isfile, mtime, size, version_id, isdelete, islatest) VALUES(?, 1, 1, ?, ?, ?, ?, ?, ?)";
		try(PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setString(1, path);
			if (isFile) {
				pstmt.setInt(2, 1);
			} else {
				pstmt.setInt(2, 0);
			}
			pstmt.setString(3, mTime);
			pstmt.setLong(4, size);
			if (versionId == null || versionId.isEmpty() || versionId.compareTo("null") == 0) {
				pstmt.setNull(5, java.sql.Types.NULL);
			} else {
				pstmt.setString(5, versionId);
			}
			if (isDelete) {
				pstmt.setInt(6, 1);
			} else { 
				pstmt.setInt(6, 0);
			}
			if (isLatest) {
				pstmt.setInt(7, 1);
			} else { 
				pstmt.setInt(7, 0);
			}
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}
	
	public static boolean updateRerunSkipObject(String jobId, String path) {
		open();
		String sql = UPDATE_JOB_ID + jobId + SQL_SKIP_CHECK_WHERE_PATH + path + "'";
		try(Statement stmt = con.createStatement()) {
			if (stmt.executeUpdate(sql) == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}

	public static boolean updateRerunSkipObjectVersion(String jobId, String path, String versionId) {
		open();
		String sql;
		if (versionId == null || versionId.isEmpty() || versionId.compareTo("null") == 0) {
			sql = UPDATE_JOB_ID + jobId + SQL_SKIP_CHECK_WHERE_PATH + path + SQL_VERSIONID_IS_NULL;
		} else {
			sql = UPDATE_JOB_ID + jobId + SQL_SKIP_CHECK_WHERE_PATH + path + SQL_VERSIONID + versionId + "'";
		}

		try(Statement stmt = con.createStatement()) {
			if (stmt.executeUpdate(sql) == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}

	public static void dropMoveObjectTable(String jobId) {
		open();
		String sql = "DROP TABLE JOB_" + jobId + "_OBJECTS";
		try(Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static void dropMoveObjectIndex(String jobId) {
		open();
		String sql = "DROP INDEX IF EXISTS PATH_" + jobId + "_INDEX";
		try(Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static synchronized void updateJobState(String jobId, IMOptions.WORK_TYPE type) {
		String sql = null;
		switch (type) {
		case MOVE:
			sql = SQL_UPDATE_JOB_STATE_MOVE;
			break;
			
		case COMPLETE:
			sql = SQL_UPDATE_JOB_STATE_COMPLETE;
			break;
			
		case STOP:
			sql = SQL_UPDATE_JOB_STATE_STOP;
			break;
			
		case REMOVE:
			sql = SQL_UPDATE_JOB_STATE_REMOVE;
			break;
			
		case RERUN:
			sql = SQL_UPDATE_JOB_STATE_RERUN;
			break;
			
		case RERUN_MOVE:
			sql = SQL_UPDATE_JOB_STATE_RERUN_MOVE;
			break;
			
		default:
			return;
		}
		open();
		try (PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setString(1, jobId);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static synchronized String getJobType(String jobId) {
		String jobType = null;
		open();
		String sql = "SELECT job_type FROM JOB WHERE job_id = " + jobId;
		try (Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(sql);) {
			if (rs.next()) {
				jobType = rs.getString(1);
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		return jobType;
	}
	
	public static synchronized void setProcessId(String jobId, String pid) {
		open();
		String sql = "UPDATE JOB SET pid = " + pid + " WHERE job_id = " + jobId;
		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static synchronized String getProcessId(String jobId) {
		String pid = null;
		Connection con = getReadConnection();
		if (con == null) {
			return null;
		}

		String sql = "SELECT pid FROM JOB WHERE job_id = " + jobId;
		try (Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(sql);) {
			if (rs.next()) {
				pid = rs.getString(1);
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		try {
			con.close();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		return pid;
	}

	public static synchronized List<Map<String, String>> status() {
		Connection con = null;
		con = getReadConnection();

		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Map<String, String> info = null;

		if (con == null) {
			return list;	
		}

		try (Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(SQL_SELECT_JOB_STATUS);) {
			while (rs.next()) {
				info = new HashMap<String, String>();
				
				info.put("jobId", rs.getString(1));
				info.put("jobState", rs.getString(2));
				info.put("jobType", rs.getString(3));
				info.put("sourcePoint", rs.getString(4));
				info.put("targetPoint", rs.getString(5));
				info.put("objectsCount", rs.getString(6));
				info.put("objectsSize", rs.getString(7));
				info.put("movedObjectsCount", rs.getString(8));
				info.put("movedObjectsSize", rs.getString(9));
				info.put("failedCount", rs.getString(10));
				info.put("failedSize", rs.getString(11));
				info.put("skipObjectsCount", rs.getString(12));
				info.put("skipObjectsSize", rs.getString(13));
				info.put("startTime", rs.getString(14));
				info.put("endTime", rs.getString(15));
				info.put("errorDesc", rs.getString(16));

				list.add(info);
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		try {
			con.close();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		
		return list;
	}
	
	public static synchronized void updateJobRerun(String jobId) {
		open();
		String sql = "UPDATE JOB SET objects_count = 0, objects_size = 0, moved_objects_count = 0, moved_objects_size = 0, failed_count = 0, failed_size = 0, skip_objects_count = 0, skip_objects_size = 0 WHERE job_id = " + jobId;
		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static void updateObjectsRerun(String jobId) {
		open();
		String sql = UPDATE_JOB_ID + jobId + "_OBJECTS SET skip_check = 0";
		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static int stateWhenExistObject(String jobId, String path) {
		open();
		int state = -1;
		String sql = "SELECT object_state FROM JOB_" + jobId + SQL_OBJECT_WHERE_PATH + path + "'";
		try (Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(sql);) {
			if (rs.next()) {
				state = rs.getInt(1);
			} 
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		return state;
	}

	public static Map<String, String> infoExistObject(String jobId, String path) {
		open();
		Map<String, String> info = new HashMap<String, String>();	
		String sql;
		sql = "SELECT object_state, mtime FROM JOB_" + jobId + SQL_OBJECT_WHERE_PATH + path + "'";

		try (Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(sql);) {
			if (rs.next()) {
				info.put("object_state", rs.getString(1));
				info.put("mtime", rs.getString(2));
			} 
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		return info;
	}

	public static Map<String, String> infoExistObjectVersion(String jobId, String path, String versionId) {
		open();
		Map<String, String> info = new HashMap<String, String>();	
		String sql;
		if (versionId == null || versionId.isEmpty() || versionId.compareTo("null") == 0) {
			sql = "SELECT object_state, mtime FROM JOB_" + jobId + SQL_OBJECT_WHERE_PATH + path + SQL_VERSIONID_IS_NULL;
		} else {
			sql = "SELECT object_state, mtime FROM JOB_" + jobId + SQL_OBJECT_WHERE_PATH + path + SQL_VERSIONID + versionId + "'";
		}

		try (Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(sql);) {
			if (rs.next()) {
				info.put("object_state", rs.getString(1));
				info.put("mtime", rs.getString(2));
			} 
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		return info;
	}
	
	public static void updateObjectInfo(String jobId, String path, String mTime, long size) {
		open();
		String sql = UPDATE_JOB_ID + jobId + "_OBJECTS SET mtime = ?, size = ? WHERE path = '" + path + "'";
		try (PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setString(1, mTime);
			pstmt.setLong(2, size);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static String getMtime(String jobId, String path) {
		open();
		String mtime = null;
		String sql = "SELECT mtime FROM JOB_" + jobId + SQL_OBJECT_WHERE_PATH + path + "'";
		try (Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(sql);) {
			if (rs.next()) {
				mtime = rs.getString(1);
			} 
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		return mtime;
	}
	
	public static synchronized boolean updateJobMoved(String jobId, long size) {
		open();
		try (PreparedStatement pstmt = con.prepareStatement(SQL_UPDATE_JOB_MOVED)) {
			pstmt.setLong(1, size);
			pstmt.setString(2, jobId);
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}
		return false;
	}

	
	public static synchronized boolean updateJobMovedCount(String jobId, long count, long size) {
		open();
		try (PreparedStatement pstmt = con.prepareStatement(SQL_UPDATE_JOB_MOVED_COUNT)) {
			pstmt.setLong(1, count);
			pstmt.setLong(2, size);
			pstmt.setString(3, jobId);
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}
		return false;
	}


	public static synchronized boolean updateObjectVersionMove(String jobId, String path, String versionId) {
		open();
		String sql;
		if (versionId == null || versionId.isEmpty() || versionId.compareTo("null") == 0) {
			sql = UPDATE_JOB_ID + jobId + "_OBJECTS SET object_state = 2 WHERE path = ? and version_id is null";
		} else {
			sql = UPDATE_JOB_ID + jobId + "_OBJECTS SET object_state = 2 WHERE path = ? and version_id = ?";
		}

		try (PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setString(1, path);
			if (versionId != null && !versionId.isEmpty() && versionId.compareTo("null") != 0) {
				pstmt.setString(2, versionId);
			} 
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}
	
	public static synchronized boolean updateObjectVersionMoveComplete(String jobId, String path, String versionId) {
		open();
		String sql;
		if (versionId == null || versionId.isEmpty() || versionId.compareTo("null") == 0) {
			sql = UPDATE_JOB_ID + jobId + "_OBJECTS SET object_state = 3 WHERE path = ? and version_id is null";
		} else {
			sql = UPDATE_JOB_ID + jobId + "_OBJECTS SET object_state = 3 WHERE path = ? and version_id = ?";
		}
		try (PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setString(1, path);
			if (versionId != null && !versionId.isEmpty() && versionId.compareTo("null") != 0) {
				pstmt.setString(2, versionId);
			} 
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}

		return false;
	}

	public static synchronized boolean updateObjectVersionMoveEventFailed(String jobId, String path, String versionId, String errorCode, String errorMessage) {
		open();
		String sql;
		if (versionId == null || versionId.isEmpty() || versionId.compareTo("null") == 0) {
			sql = UPDATE_JOB_ID + jobId + "_OBJECTS SET object_state = 4, error_date = datetime('now', 'localtime'), error_code = ?, error_desc = ? WHERE path = ? and version_id is null";
		} else {
			sql = UPDATE_JOB_ID + jobId + "_OBJECTS SET object_state = 4, error_date = datetime('now', 'localtime'), error_code = ?, error_desc = ? WHERE path = ? and version_id = ?";
		}

		try (PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setString(1, errorCode);
			pstmt.setString(2, errorMessage);
			pstmt.setString(3, path);
			if (versionId != null && !versionId.isEmpty() && versionId.compareTo("null") != 0) {
				pstmt.setString(4, versionId);
			} 
			if (pstmt.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		return false;
	}
	
	public static void insertErrorJob(String jobId, String msg) {
		open();
		try (PreparedStatement pstmt = con.prepareStatement(SQL_UPDATE_JOB_ERROR + jobId)) {
			pstmt.setInt(1, JOB_ERROR);
			pstmt.setString(2, msg);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static void deleteCheckObjects(String jobId) {
		open();
		String sql = "DELETE FROM JOB_" + jobId + "_OBJECTS WHERE skip_check = 0";
		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}

	public static synchronized List<Map<String, String>> getToMoveObjectsInfo(String jobId, long sequence, long limit) {
		open();
		String sql = "SELECT path, isfile, size, version_id, isdelete, islatest FROM JOB_" + jobId + "_OBJECTS WHERE object_state = 1 and sequence > " + sequence + " ORDER BY sequence LIMIT " + limit;
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		try (Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(sql);) {
	   		while (rs.next()) {
		   		Map<String, String> info = new HashMap<String, String>();
		   
		   		info.put("path", rs.getString(1));
		   		info.put("isFile", rs.getString(2));
		   		info.put("size", rs.getString(3));
		   		info.put("versionId", rs.getString(4));
		   		info.put("isDelete", rs.getString(5));
				info.put("isLatest", rs.getString(6));
				
		   		list.add(info);
			}
	   	} catch (SQLException e) {
	   		logger.error(e.getMessage());
   		}
   
   		return list;
	}
	
	public static void updateJobStart(String jobId) {
		open();
		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(SQL_UPDATE_JOB_START + jobId);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}

	public static void updateJobEnd(String jobId) {
		open();
		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(SQL_UPDATE_JOB_END + jobId);
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}

	public static long getMaxSequence(String jobId) {
		open();
		long maxSequence = 0;
		String sql = "SELECT MAX(sequence) FROM JOB_" + jobId + "_OBJECTS";
		try (Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(sql);) {
			if (rs.next()) {
				maxSequence = rs.getLong(1);
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		return maxSequence;
	}
}
