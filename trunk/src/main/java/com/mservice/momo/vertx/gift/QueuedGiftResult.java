package com.mservice.momo.vertx.gift;

import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.QueuedGift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nam on 11/6/14.
 */
public class QueuedGiftResult {
    public List<QueuedGift> queuedGifts = new ArrayList<>();
    public Map<String, Gift> gifts = new HashMap<>();
}
