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

    public static JDA jda;

    public static void main(String[] args) throws LoginException, InterruptedException {

        new Musikroboter();

    }

    private Musikroboter() throws LoginException, InterruptedException {

        Scanner token = new Scanner(System.in);
        jda = JDABuilder.createDefault(token.nextLine().trim())
                .enableCache(CacheFlag.VOICE_STATE)
                .setActivity(Activity.listening("/help"))
                .addEventListeners(new Listener())
                .build().awaitReady();
        for(Guild g : jda.getGuilds()) addSlashCommands(g);
        Scanner sc = new Scanner(System.in);
        while(true) {
            String str = sc.nextLine();
            if(str.toLowerCase().matches("^s[hutdown]*")) {
                jda.getPresence().setPresence(OnlineStatus.IDLE, Activity.playing("shutting down..."));
                break;
            }
        }

        sc.close();
        jda.cancelRequests();
        jda.shutdown();
        System.exit(0);

    }

    private void addSlashCommands(Guild g) {

        if(g == null) return;

        g.upsertCommand("join", "It is going to join your channel if it isn't preoccupied").queue();
        g.upsertCommand("quit", "It is going to leave your channel if it hasn't left already").queue();
        g.upsertCommand("play","It is going to play the given music")
                .addOption(OptionType.STRING,"title","Takes in a link or title referring to the content", false).queue(); // req
        g.upsertCommand("pause","It is going to take a break from playing")
                .addOption(OptionType.INTEGER, "duration", "For how long the bot should stop playing", false)
                .addOption(OptionType.STRING, "timeunit", "Duration in Seconds, Minutes, Hours, Days, ...?", false).queue();
        g.upsertCommand("queue", "It is going to show the playlist").queue();
        g.upsertCommand("skip", "It is going to skip the music currently playing")
                        .addOption(OptionType.INTEGER, "amount", "Amount of tracks to skip", false).queue();
        g.upsertCommand("volume","It is going to regulate the volume")
                .addOption(OptionType.INTEGER, "volume", "Volume in percent", false).queue(); // req
        g.upsertCommand("stop","It is going to stop playing").queue();
        g.upsertCommand("jump","It is going to jump the given amount of seconds")
                .addOption(OptionType.INTEGER, "seconds", "Amount of seconds to jump forward to", false).queue(); /// req
        g.upsertCommand("shuffle","Shuffles the playlist").queue();
        g.upsertCommand("loop","Repeats the current track or playlist")
                .addOption(OptionType.BOOLEAN,"track","Whether it should repeat the playlist or just the playing track",false).queue();
        g.upsertCommand("bass", "Boosts the bass of the original track by the given amount")
                        .addOption(OptionType.INTEGER, "amount", "Percentage of bass boost", false).queue(); //req
        g.upsertCommand("info","Shows information about the current track").queue();
        g.upsertCommand("help","It is going to send you instructions of how you're intended to use it").queue();

    }

}
