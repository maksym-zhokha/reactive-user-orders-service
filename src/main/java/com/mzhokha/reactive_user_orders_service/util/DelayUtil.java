package com.mzhokha.reactive_user_orders_service.util;

import static java.lang.Thread.sleep;

public class DelayUtil {

    public static void delay(int ms){
        try {
            sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
