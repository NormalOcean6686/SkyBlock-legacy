/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2018 larryTheCoder and contributors
 *
 * Permission is hereby granted to any persons and/or organizations
 * using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or
 * any derivatives of the work for commercial use or any other means to generate
 * income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing
 * and/or trademarking this software without explicit permission from larryTheCoder.
 *
 * Any persons and/or organizations using this software must disclose their
 * source code and have it publicly available, include this license,
 * provide sufficient credit to the original authors of the project (IE: larryTheCoder),
 * as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,FITNESS FOR A PARTICULAR
 * PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.larryTheCoder.command.island;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import com.larryTheCoder.ASkyBlock;
import com.larryTheCoder.command.SubCommand;

/**
 * @author larryTheCoder
 */
public class InfoSubCommand extends SubCommand {

    public InfoSubCommand(ASkyBlock plugin) {
        super(plugin);
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission("is.command.info") && sender.isPlayer();
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Get the island info where you standing at";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"inf", "get"};
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player p = sender.getServer().getPlayer(sender.getName());
        for (String level : getPlugin().getLevels()) {
            if (!p.getLevel().getName().equalsIgnoreCase(level)) {
                sender.sendMessage(getPrefix() + getLocale(p).errorWrongWorld);
                return true;
            }
        }
        getPlugin().getIsland().islandInfo(p, p.getLocation());
        return true;
    }

}
