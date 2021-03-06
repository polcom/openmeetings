/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.service.quartz.scheduler;

import static org.apache.openmeetings.util.OmFileHelper.EXTENSION_MP4;
import static org.apache.openmeetings.util.OmFileHelper.TEST_SETUP_PREFIX;
import static org.apache.openmeetings.util.OmFileHelper.getStreamsDir;
import static org.apache.openmeetings.util.OpenmeetingsVariables.isInitComplete;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.openmeetings.db.dao.server.SessiondataDao;
import org.apache.openmeetings.db.dao.user.UserDao;
import org.apache.openmeetings.db.dto.room.Whiteboard;
import org.apache.openmeetings.db.dto.room.Whiteboards;
import org.apache.openmeetings.db.entity.user.User;
import org.apache.openmeetings.db.manager.IStreamClientManager;
import org.apache.openmeetings.db.manager.IWhiteboardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class CleanupJob extends AbstractJob {
	private static Logger log = LoggerFactory.getLogger(CleanupJob.class);
	private long sessionTimeout = 30 * 60 * 1000L;
	private long testSetupTimeout = 60 * 60 * 1000L; // 1 hour
	private long roomFilesTtl = 60 * 60 * 1000L; // 1 hour
	private long resetHashTtl = 24 * 60 * 60 * 1000L; // 1 day

	@Autowired
	private SessiondataDao sessionDao;
	@Autowired
	private IStreamClientManager streamClientManager;
	@Autowired
	private UserDao userDao;
	@Autowired
	private IWhiteboardManager wbManager;

	public void setSessionTimeout(long sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public void setTestSetupTimeout(long testSetupTimeout) {
		this.testSetupTimeout = testSetupTimeout;
	}

	public void setRoomFilesTtl(long roomFilesTtl) {
		this.roomFilesTtl = roomFilesTtl;
	}

	public void setResetHashTtl(long resetHashTtl) {
		this.resetHashTtl = resetHashTtl;
	}

	public void cleanTestSetup() {
		log.trace("CleanupJob.cleanTestSetup");
		final long now = System.currentTimeMillis();
		if (!isInitComplete()) {
			return;
		}
		try {
			File[] folders = getStreamsDir().listFiles(File::isDirectory);
			if (folders == null) {
				return;
			}
			for (File folder : folders) {
				File[] files = folder.listFiles(
						fi -> fi.getName().startsWith(TEST_SETUP_PREFIX) && fi.isFile() && fi.lastModified() + testSetupTimeout < now
					);
				if (files == null) {
					continue;
				}
				for (File file : files) {
					log.debug("expired TEST SETUP found: " + file.getCanonicalPath());
					file.delete();
				}
			}
		} catch (Exception e) {
			log.error("Unexpected exception while processing tests setup videous.", e);
		}
	}

	public void cleanRoomFiles() {
		log.trace("CleanupJob.cleanRoomFiles");
		final long now = System.currentTimeMillis();
		if (!isInitComplete()) {
			return;
		}
		try {
			File[] folders = getStreamsDir().listFiles(File::isDirectory);
			if (folders == null) {
				return;
			}
			for (File folder : folders) {
				Long roomId = null;
				if (NumberUtils.isCreatable(folder.getName())) {
					roomId = Long.valueOf(folder.getName());
					Whiteboards wbList = wbManager.get(roomId);
					for (Map.Entry<Long, Whiteboard> e : wbList.getWhiteboards().entrySet()) {
						if (!e.getValue().isEmpty()) {
							roomId = null;
							break;
						}
					}
				}
				if (roomId != null && streamClientManager.list(roomId).isEmpty()) {
					File[] files = folder.listFiles(fi -> fi.isFile() && fi.lastModified() + roomFilesTtl < now);
					if (files != null && files.length > 0) {
						log.debug("Room files are too old and no users in the room: " + roomId);
						FileUtils.deleteDirectory(folder);
					}
				}
			}
		} catch (Exception e) {
			log.error("Unexpected exception while processing tests setup videous.", e);
		}
	}

	public void cleanSessions() {
		log.trace("CleanupJob.cleanSessions");
		if (!isInitComplete()) {
			return;
		}
		try {
			sessionDao.clearSessionTable(sessionTimeout);
		} catch (Exception err){
			log.error("execute",err);
		}
	}

	public void cleanExpiredRecordings() {
		log.trace("CleanupJob.cleanExpiredRecordings");
		processExpiringRecordings(true, (rec, days) -> {
			if (days < 0) {
				log.debug("cleanExpiredRecordings:: following recording will be deleted {}", rec);
				File f = rec.getFile(EXTENSION_MP4);
				if (f != null && f.exists()) {
					f.delete();
				}
				recordingDao.delete(rec);
			}
		});
	}

	public void cleanExpiredResetHash() {
		log.trace("CleanupJob.cleanExpiredResetHash");
		if (!isInitComplete()) {
			return;
		}
		List<User> users = userDao.getByExpiredHash(resetHashTtl);
		if (!users.isEmpty()) {
			log.debug("... {} expired hashes were found", users.size());
			for (User u : users) {
				u.setResetDate(null);
				u.setResethash(null);
				userDao.update(u, null);
			}
			log.debug("... DONE CleanupJob.cleanExpiredResetHash");
		}
	}
}
