package com.tiza.service.support.client;

import cn.com.tiza.tstar.common.Configuration;
import cn.com.tiza.tstar.common.alarm.TStarAlarmer;
import cn.com.tiza.tstar.common.alarm.WatchDog;
import cn.com.tiza.tstar.common.datasource.TStarDBManager;
import cn.com.tiza.tstar.datainterface.client.entity.ClientCmdSendResult;
import cn.com.tiza.tstar.datainterface.service.InterfaceService;
import cn.com.tiza.tstar.datainterface.service.ServerException;
import cn.com.tiza.tstar.datainterface.service.entity.CmdSendResult;
import cn.com.tiza.tstar.datainterface.service.entity.UserInfo;
import cn.com.tiza.tstar.datainterface.service.impl.InterfaceServiceImpl;
import cn.com.tiza.tstar.datainterface.service.impl.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.ConfigurationException;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Description: TStarClientAdapter
 * Author: DIYILIU
 * Update: 2019-04-22 09:39
 */

@Slf4j
public class TStarClientAdapter{
    private InterfaceService interfaceService;
    private UserInfo userInfo;
    private String userName;
    private String password;
    private String filePath;

    public TStarClientAdapter(String userName, String password, String filePath){
        this.userName = userName;
        this.password = password;
        this.filePath = filePath;
    }

    public void init() throws ServerException {
        try {
            String path1 = "common.xml";
            String path2 = "datainterface-client.xml";
            if (filePath != null){
                path1 = filePath + path1;
                path2 = filePath + path2;
            }

            Configuration.getInstance().load(path1);
            Configuration.getInstance().load(path2);
        } catch (ConfigurationException var6) {
            log.error(var6.getMessage());
            throw new ServerException("Load configuration has failed!", var6);
        }

        log.info("Load common.xml,datainterface-client.xml is successful.");
        if (!TStarDBManager.getInstance().isAvailable()) {
            log.error("Init Database connection pool has failed!");
            throw new ServerException("Init Database connection pool has failed!");
        } else {
            log.info("Init Database connection pool has completed.");
            TStarAlarmer.init("datainterface-clientsimple");
            ScheduledExecutorService watchDogService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    return new Thread(r, "WatchDog");
                }
            });
            watchDogService.scheduleAtFixedRate(new WatchDog(), 1L, (long)Configuration.getInstance().getInt("tstar.datainterface.client.watchdog.interval", 60), TimeUnit.SECONDS);
            this.interfaceService = new InterfaceServiceImpl();
            log.info("Interface service init success, impl={}.", this.interfaceService.getClass().getName());

            try {
                this.userInfo = LoginService.getInstance().validateUser(userName, password);
            } catch (SQLException var5) {
                log.error(var5.getMessage(), var5);
                throw new ServerException("Valid username and password has failed!", var5);
            }

            if (this.userInfo != null) {
                log.info("The user:" + this.userName + " has login!");
            } else {
                log.error("The username:" + this.userName + " has failed to login!");
                throw new ServerException("Username or password is not valid!");
            }
        }
    }

    public ClientCmdSendResult cmdSend(String terminalType, String terminalID, int cmdID, int cmdSerial, byte[] cmdBody, int cmdType) throws ServerException {
        CmdSendResult result;

        try {
            result = this.interfaceService.cmdSend(this.userInfo, terminalType, terminalID, cmdID, cmdSerial, cmdBody, cmdType);
        } catch (ServerException var10) {
            throw var10;
        }

        ClientCmdSendResult clientResult = new ClientCmdSendResult();
        boolean isSuccess = result.isSuccess();
        clientResult.setIsSuccess(isSuccess);
        if (isSuccess) {
            clientResult.setCmdCheckId(result.getCmdCheckId());
        } else {
            clientResult.setErrorCode(result.getErrorCode());
        }

        return clientResult;
    }
}
