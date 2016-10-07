package com.mservice.momo.cmd;

import com.mservice.momo.data.*;
import com.mservice.momo.data.web.ArticleDb;
import com.mservice.momo.data.web.BankAccountDb;
import com.mservice.momo.data.web.KeyValueDb;

/**
 * Created by ntunam on 3/10/14.
 */
public class MainDb {
    public PhonesDb mPhonesDb;
    public ImeisDb mImeisDb;
    public BillsDb mBillsDb;
    public AccessHistoryDb mAccessHistoryDb;
    public DeviceInfoDb mDeviceInfoDb;
    public AgentsDb mAgentDb;
    public TransDb transDb;

    public FeeDb feeDb;

    //web
    public ArticleDb articleDb;
    public BankAccountDb bankAccountDb;
    public KeyValueDb keyValueDb;
}
