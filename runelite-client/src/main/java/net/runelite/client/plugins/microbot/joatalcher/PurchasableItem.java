package net.runelite.client.plugins.microbot.joatalcher;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchasableItem {

    String name;
    int max_price, itemId,gelimit;

    public PurchasableItem(String name, int itemId, int max_price, int ge_limit) {
        this.name = name;
        this.itemId = itemId;
        this.max_price = max_price;
        this.gelimit = ge_limit;

    }

}
