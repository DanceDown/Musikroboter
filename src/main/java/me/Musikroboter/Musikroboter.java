package me.Musikroboter;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.util.Scanner;

public class Musikroboter {

    static JDA jda;

    public static void main(String[] args) throws LoginException, InterruptedException {

        new Musikroboter();

    }

    private Musikroboter() throws LoginException, InterruptedException {

        jda = JDABuilder.createDefault("Nzc5MDI4Mzc5NjMzOTc1MzE3.Ge-AZE.8qmmi-khWtxhCny-WPXG98kTWzkHuj5-Mc6lhA")
                .enableCache(CacheFlag.VOICE_STATE)
                .setActivity(Activity.listening("/help"))
                .addEventListeners(new Listener())
                .build().awaitReady();

        for(Guild g : jda.getGuilds()) addSlashCommands(g);

        Scanner sc = new Scanner(System.in);
        while(true) {
            String str = sc.next();
            System.out.println(str);
            if(str.toLowerCase().matches("^s[hutdown]*")) {
                jda.getPresence().setPresence(OnlineStatus.IDLE, Activity.playing("shutting down..."));
                break;
            }
        }

        jda.cancelRequests();
        jda.shutdown();
        System.exit(0);

    }

    private void addSlashCommands(Guild g) {

        if(g == null) return;

        g.upsertCommand("join", "It is going to join your channel if it isn't preoccupied").queue();
        g.upsertCommand("quit", "It is going to leave your channel if it hasn't left already").queue();
        g.upsertCommand("play","It is going to play the given music")
                .addOption(OptionType.STRING,"title","Takes in a link or title referring to the content", true).queue();
        g.upsertCommand("pause","It is going to take a break from playing")
                .addOption(OptionType.INTEGER, "duration", "For how long the bot should stop playing", false)
                .addOption(OptionType.STRING, "timeunit", "Duration in Seconds, Minutes, Hours, Days, ...?", false).queue();
        g.upsertCommand("queue", "It is going to show the playlist").queue();
        g.upsertCommand("skip", "It is going to skip the music currently playing")
                        .addOption(OptionType.INTEGER, "amount", "Amount of tracks to skip", false).queue();
        g.upsertCommand("volume","It is going to regulate the volume")
                .addOption(OptionType.INTEGER, "volume", "Volume in percent", true).queue();
        g.upsertCommand("stop","It is going to stop playing").queue();
        g.upsertCommand("jump","It is going to jump the given amount of seconds")
                .addOption(OptionType.INTEGER, "seconds", "Amount of seconds to jump forward to", true).queue();
        g.upsertCommand("shuffle","Shuffles the playlist").queue();
        g.upsertCommand("loop","Repeats the current track or playlist")
                .addOption(OptionType.BOOLEAN,"track","Whether it should repeat the playlist or just the playing track",false).queue();
        g.upsertCommand("help","It is going to send you instructions of how you're intended to use it").queue();

    }

}
