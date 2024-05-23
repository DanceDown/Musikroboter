package me.Musikroboter;

import dev.arbjerg.lavalink.client.Helpers;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.NodeOptions;
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.Scanner;

public class Musikroboter {

    private static JDA jda;
    private static LavalinkClient client;

    public static void main(String[] args) throws InterruptedException {

        new Musikroboter();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            PlayerManager.getINSTANCE().getTrackHandlers().forEach(((guildID, trackHandler) -> trackHandler.stop()));
            jda.cancelRequests();
            client.close();
            jda.shutdown();
            System.exit(0);
        }));

    }

    private Musikroboter() throws InterruptedException {

        System.out.print("Enter the token:\t");
        Scanner sc = new Scanner(System.in);
        final String token = sc.nextLine().trim();
        System.out.print("Enter the password:\t");
        final String password = sc.nextLine().trim();
        sc.close();

        client = new LavalinkClient(Helpers.getUserIdFromToken(token));
        client.addNode(new NodeOptions.Builder()
                .setName("Server")
                .setServerUri("http://127.0.0.1:2333")
                .setPassword(password)
                .build());

        jda = JDABuilder.createDefault(token)
                .enableCache(CacheFlag.VOICE_STATE)
                .setActivity(Activity.listening("/help"))
                .addEventListeners(new Listener())
                .setVoiceDispatchInterceptor(new JDAVoiceUpdateListener(client))
                .build().awaitReady();
        for(Guild g : jda.getGuilds())
            addSlashCommands(g);

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
        g.upsertCommand("pan", "Rotates the sound around your ears")
                .addOption(OptionType.NUMBER, "speed", "Frequency with 100 being 1Hz", true).queue();
        g.upsertCommand("karaoke", "Activates karaoke mode")
                .addOption(OptionType.INTEGER, "level", "How strong the vocals are suppressed", false)
                .addOption(OptionType.INTEGER, "mono", "How mono the music is", false)
                .addOption(OptionType.NUMBER, "band", "at what Hz the vocals are", false)
                .addOption(OptionType.NUMBER, "width", "how wide the vocals are going to be suppressed", false).queue();
        g.upsertCommand("speed", "how fast the track should be played")
                .addOption(OptionType.NUMBER, "speed", "What to multiply the base speed with", true)
                .addOption(OptionType.NUMBER, "pitch", "What the new pitch should be", false)
                .addOption(OptionType.NUMBER, "rate", "At what rate to play", false).queue();
        g.upsertCommand("help","It is going to send you instructions of how you're intended to use it").queue();

    }

    public static LavalinkClient getClient() {
        return client;
    }

    public static JDA getJda() {
        return jda;
    }
}
