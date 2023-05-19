/*
 * Copyright 2016 JiongBull
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package framework.telegram.support.system.log.core.storage;


import java.io.File;

import androidx.annotation.NonNull;
import framework.telegram.support.system.log.core.IStorage;
import framework.telegram.support.system.log.core.Logger;
import framework.telegram.support.system.log.core.utils.FileUtils;
import framework.telegram.support.system.log.core.utils.LogUtils;

/**
 * 设置日志的存储空间，超过容量后会按日志的最后修改时间清理，时间越早的越先被清理掉，
 * 大约清理出一半配置空间的时停止.
 */
public class DiskStorage implements IStorage {

    private final DiskConfigs mDiskConfigs;

    public DiskStorage(@NonNull DiskConfigs diskConfigs) {
        mDiskConfigs = diskConfigs;
    }

    @Override
    public void upload(@NonNull Logger logger) {
        String logDirName = logger.getLogDir();
        String logDirPath = LogUtils.genDirPath(logDirName);
        File logDir = new File(logDirPath);

        if (FileUtils.isExist(logDir)) {
            if (FileUtils.calSize(logDir) > mDiskConfigs.getCapacity()) {
                File[] sourceFiles = logDir.listFiles();
                FileUtils.sortByModifyDateDesc(sourceFiles);

                long size = 0;
                for (File file : sourceFiles) {
                    size += FileUtils.calSize(file);
                    FileUtils.delete(file);
                    if (size > mDiskConfigs.getCapacity() / 2) {
                        break;
                    }
                }
            }
        }
    }
}