package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.data.AgentsDb;
import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.msg.MomoCommand;
import com.mservice.momo.msg.MomoMessage;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.List;

/**
 * Created by ntunam on 3/27/14.
 */
public class StoresHandler extends CommandHandler {
    public StoresHandler(MainDb mainDb, Vertx vertx, Container container, JsonObject config) {
        super(mainDb, vertx, container, config);
    }

    public void getStoreAround(final CommandContext context) {
        CmdModels.GetStoreAround cmd = (CmdModels.GetStoreAround) context.getCommand().getBody();

        mainDb.mAgentDb.getStores(cmd.getLongitude(), cmd.getLatitude(), cmd.getDistrictId(), cmd.getCityId(), cmd.getLimit(), new org.vertx.java.core.Handler<List<AgentsDb.StoreInfo>>() {
            @Override
            public void handle(List<AgentsDb.StoreInfo> objects) {
                CmdModels.GetStoreAroundReply.Builder builder = CmdModels.GetStoreAroundReply.newBuilder();

                if (objects != null) {
                    for (AgentsDb.StoreInfo item : objects) {

                        builder.addStores(CmdModels.StoreInfo.newBuilder()
                                .setName(item.storeName)
                                .setStreet(item.street)
                                .setOwnerName(item.name + MomoMessage.BELL + item.wardname)
                                .setOwnerPhone(item.phone)
                                .setLocation(CmdModels.Location.newBuilder()
                                        .setLongitude(item.loc.Lng)
                                        .setLatitude(item.loc.Lat))
                                .setAddress(item.address)
                                .setDistrictId(item.districtId)
                                .setCityId(item.cityId)
                                .setAreaId(item.areaId));
                    }
                }

                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_STORE_AROUND_REPLY, builder.build());
                context.reply(replyCommand);
            }
        });
    }

    public void getStoreByCode(final CommandContext context) {
        final CmdModels.GetStoreByCode cmd = (CmdModels.GetStoreByCode) context.getCommand().getBody();

        if (cmd.getPageSize() == 0) {
            context.replyError(1, "PageSize must be greater than 0.");
            return;
        }

        mainDb.mAgentDb.countStoreByCode(cmd.getCid(), cmd.getDid(), cmd.getPageSize(), cmd.getPageNum(), new Handler<Long>() {
            @Override
            public void handle(Long size) {
                long count = size / cmd.getPageSize();

                if(size % cmd.getPageSize() > 0)
                    count ++;
                final long pageCount = count;


                mainDb.mAgentDb.getStores(cmd.getCid(), cmd.getDid(), cmd.getPageSize(), cmd.getPageNum(), new Handler<List<AgentsDb.StoreInfo>>() {
                    @Override
                    public void handle(List<AgentsDb.StoreInfo> objects) {
                        CmdModels.GetStoreByCodeReply.Builder builder = CmdModels.GetStoreByCodeReply.newBuilder()
                                .setPageCount(pageCount);

                        if (objects != null) {
                            for (AgentsDb.StoreInfo item : objects) {
                                builder.addStores(CmdModels.StoreInfo.newBuilder()
                                        .setName(item.storeName)
                                        .setOwnerName(item.name + MomoMessage.BELL + item.wardname)
                                        .setOwnerPhone(item.phone)
                                        .setWardName(item.wardname)
                                        .setLocation(CmdModels.Location.newBuilder()
                                                .setLongitude(item.loc.Lng)
                                                .setLatitude(item.loc.Lat))
                                        .setAddress(item.address)
                                        .setDistrictId(item.districtId)
                                        .setCityId(item.cityId)
                                        .setAreaId(item.areaId)
                                        .setStreet(item.street));

                            }
                        }

                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_STORE_BY_CODE_REPLY, builder.build());

                        context.reply(replyCommand);
                    }
                });
            }
        });
    }
}
