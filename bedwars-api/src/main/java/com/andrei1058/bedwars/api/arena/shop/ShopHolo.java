/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.api.arena.shop;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.messages.ChineseMessages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ShopHolo {
    /**
     * 商店共用的中文全息字
     */
    private static List<ShopHolo> shopHolo = new ArrayList<>();

    private ArmorStand a1, a2;
    private Location l;
    private IArena a;

    public ShopHolo(ArmorStand a1, ArmorStand a2, Location l, IArena a) {
        this.l = l;
        for (ShopHolo sh : getShopHolo()) {
            if (sh.l.equals(l)) {
                if (a1 != null) a1.remove();
                if (a2 != null) a2.remove();
                return;
            }
        }
        this.a1 = a1;
        this.a2 = a2;
        this.a = a;
        if (a1 != null) a1.setMarker(true);
        if (a2 != null) a2.setMarker(true);
        shopHolo.add(this);
    }

    public static void clearForArena(IArena a) {
        for (ShopHolo sh : new ArrayList<>(getShopHolo())) {
            if (sh.a == a) {
                if (sh.a1 != null) sh.a1.remove();
                if (sh.a2 != null) sh.a2.remove();
                shopHolo.remove(sh);
            }
        }
    }

    public IArena getA() {
        return a;
    }

    public static List<ShopHolo> getShopHolo() {
        return shopHolo;
    }
}
