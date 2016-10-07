package com.mservice.momo.vertx.retailer;

import com.mservice.momo.data.model.Const;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by nam on 6/15/14.
 */
public class RetailerFeeVerticle extends Verticle {

    private static ArrayList<FeeObj> arrayListFEE = new ArrayList<>();
    private Logger logger;
    private JsonObject glbCfg;

	@Override
	public void start() {
        this.logger = container.logger();
        this.glbCfg = container.config();
        if (arrayListFEE.size() == 0) {
            readAllFilesFromDir("feeofretailter", logger);
        }

		vertx.eventBus().registerHandler(AppConstant.Retailer_Fee_Address,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
                        FeeObj feeObj = new FeeObj(message.body());


						switch (feeObj.command) {
							case FeeObj.get_fee:
								getGetFee(message, feeObj);
								break;

							default:
								container.logger().info("RetailerFeeVerticle  not suport for the command " + feeObj.command);
								message.reply(new JsonObject());
								break;
						}
					}
				});
	}

    private void getGetFee(Message<JsonObject> message, FeeObj feeReqObj) {

        long tranAmt = feeReqObj.tranAmount;
        int tranType = feeReqObj.tranType;

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + feeReqObj.phoneNumber);
        log.add("trantype", MomoProto.TranHisV1.TranType.valueOf(tranType));
        log.add("amount", tranAmt);

        FeeObj feeItem = null;
        for(int i =0;i< arrayListFEE.size();i++){
            feeItem = arrayListFEE.get(i);
            if((feeItem.from <= tranAmt) && (tranAmt<= feeItem.to) && (feeItem.tranType == tranType)){
                break;
            }
        }

        feeItem = (feeItem == null ? new FeeObj() : feeItem);

        log.add("retailer get fee", "begind");
        log.add("trantype", MomoProto.TranHisV1.TranType.valueOf(tranType));
        log.add("value from", feeItem.from);
        log.add("value to", feeItem.to);
        log.add("fee", feeItem.fee);
        log.add("retailer get fee", "end");

        message.reply(feeItem.toJson());
        log.writeLog();
    }

    private void readAllFilesFromDir(String dir, Logger logger){
        String filePath = dir + "/fee.json";
        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(dir);
        readOneFile(filePath, log);
        log.add("end","done");
        log.writeLog();
    }

    private void readOneFile(String fileName, Common.BuildLog log){
        BufferedReader br = null;
        try {

			try {
				br = new BufferedReader(new FileReader(fileName));
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			StringBuilder sb = new StringBuilder();
			String line = null;
			try {
				line = br.readLine();
				while (line != null) {
					sb.append(line);
					sb.append("\n");
					line = br.readLine();
				}
				String fullContent = sb.toString();
				JsonArray jsonArray = new JsonArray(fullContent);

                if(jsonArray != null && jsonArray.size() > 0){

                    for (int i =0; i< jsonArray.size(); i++){
                        JsonObject jo = jsonArray.get(i);
                        arrayListFEE.add(new FeeObj(jo));
                    }
                }

            }catch (IOException e) {
                e.printStackTrace();
                log.add("error", e.getMessage());
            }

        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                log.add("error", e.getMessage());
            }
        }
    }


}
