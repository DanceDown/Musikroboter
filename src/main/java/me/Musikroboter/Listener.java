package me.Musikroboter;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class Listener extends ListenerAdapter {

    AudioChannel activeVChannel;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent ev) {

        Member m = ev.getMember();
        Guild g = ev.getGuild();

        if(g == null || m == null) {
            sendMessage(ev, "Please do not DM me!", "Instead use these commands on servers. Don't worry, only you can see my responses.", Color.RED);
            return;
        }

        MusicManager musicManager = PlayerManager.getINSTANCE().getMusicManager(g);

        HashMap<String, String> args = new HashMap<>();
        for(OptionMapping opt : ev.getOptions()) args.put(opt.getName(), opt.getAsString());

        String name = ev.getName().toLowerCase();

        switch (name) {
            case "join" -> {

                if(m.getVoiceState() == null || m.getVoiceState().getChannel() == null) {
                    sendMessage(ev, "Where?", "You are currently in no channel 🙃", Color.RED);
                    return;
                }

                if (!m.getVoiceState().getChannel().equals(activeVChannel) && activeVChannel.getMembers().size() > 1) {
                    sendMessage(ev, "Sorry", "I can't do that whilst playing for somebody else! 💔", Color.RED);
                    return;
                }

                sendMessage(ev, "Greetings", "How can I help you? 😃", Color.GREEN);
                g.getAudioManager().openAudioConnection(activeVChannel = m.getVoiceState().getChannel());
                Musikroboter.jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
            }
            case "quit" -> {
                if (activeVChannel == null) {
                    sendMessage(ev, "Jokes on you", "I'm currently in no channel! 👊", Color.RED);
                    return;
                }
                sendMessage(ev, "Goodbye", "Have a great day! 👋", Color.MAGENTA);
                activeVChannel = null;
                PlayerManager.getINSTANCE().getMusicManager(g).handler.stop();
                g.getAudioManager().closeAudioConnection();
                Musikroboter.jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.listening("/help"));
            }
            case "help" -> ev.replyEmbeds(new EmbedBuilder()
                    .setColor(Color.MAGENTA)
                    .setTitle("Help")
                    .addField("/join", "Joining the channel", true)
                    .addField("/quit", "Leaving the channel", true)
                    .addField("/play [Title/Link]", "Playing the given track(s)", true)
                    .addField("/pause {Duration}", "Takes a break", true)
                    .addField("/queue", "Shows the playlist", true)
                    .addField("/skip", "Skips the current track", true)
                    .addField("/volume [Percent]", "Regulates the volume", true)
                    .addField("/jump [Seconds]", "Skips the amount of seconds of the current track", true)
                    .addField("/shuffle", "Shuffles the playlist", true)
                    .addField("/repeat {Playlist}", "Repeats the current track or playlist", true)
                    .addField("/stop", "Stops playing", true)
                    .addField("/info", "shows the detailed information about the current track", true)
                    .addField("/help", "Shows this list", true).build()).setEphemeral(true).queue();
            case "play" -> {
                if(activeVChannel == null) {
                    if(m.getVoiceState() == null || m.getVoiceState().getChannel() == null) {
                        sendMessage(ev, "Where?", "You are currently in no channel 🙃", Color.RED);
                        return;
                    } else g.getAudioManager().openAudioConnection(activeVChannel = m.getVoiceState().getChannel());
                }
                String link = args.get("title");
                ev.deferReply(true).queue();
                PlayerManager.getINSTANCE().load(ev.getTextChannel(), ev.getHook(), link);
            }
            case "stop" -> {
                PlayerManager.getINSTANCE().getMusicManager(g).handler.stop();
                sendMessage(ev, "Stopping", "Would you like me to play something else instead?", Color.GREEN);
                Musikroboter.jda.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.listening("/help"));
            }
            case "pause" -> {
                if (args.get("duration") != null) {

                    int duration;

                    try {
                        duration = Integer.parseInt(args.get("duration"));
                        if (duration <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException ex) {
                        sendMessage(ev, "Duration invalid!", "`" + args.get("duration") +  "` is not a valid integer number!", Color.RED);
                        return;
                    }

                    TimeUnit time;
                    String timeunit;
                    if ((timeunit = args.get("timeunit")) != null) {
                        if (timeunit.toLowerCase().matches("^se?[ckx]?s?")) time = TimeUnit.SECONDS;
                        else if (timeunit.toLowerCase().matches("^m(ins?)?")) time = TimeUnit.MINUTES;
                        else if (timeunit.toLowerCase().matches("^(h(ours?)?)|st")) time = TimeUnit.HOURS;
                        else if (timeunit.toLowerCase().matches("^(d(ays?)?)|t")) time = TimeUnit.DAYS;
                        else {
                            sendMessage(ev, "Unit not supported", "Please choose between seconds, minutes, hours and days! ❌", Color.RED);
                            return;
                        }
                    } else time = TimeUnit.SECONDS;
                    boolean paused = musicManager.handler.pause(duration, time);
                    if (paused) {
                        sendMessage(ev, "Pausing", String.join(" ", "The bot is going to pause in", String.valueOf(duration), time.name(), " ⏰"), Color.GREEN);
                    } else {
                        sendMessage(ev, "Continuing", String.join(" ", "The bot is going to continue playing in", String.valueOf(duration), time.name(), " ⏰"), Color.GREEN);
                    }
                } else {
                    boolean paused = musicManager.handler.pause();
                    sendMessage(ev, paused ? "Pausing" : "Continuing", paused ? "Time to take a break!" : "Back to business", Color.GREEN);
                }
            }
            case "shuffle" -> {
                musicManager.handler.shuffle();
                sendMessage(ev, "Shuffling", "The list has successfully been shuffled", Color.GREEN);
            }
            case "loop" -> {
                Map.Entry<Boolean, Boolean> looped = musicManager.handler.isLooped();
                if (args.get("track") == null) {
                    musicManager.handler.setLooped(!looped.getKey(), looped.getValue());
                } else {
                    musicManager.handler.setLooped(true, Boolean.parseBoolean(args.get("track").toLowerCase()));
                }
                looped = musicManager.handler.isLooped();
                if (looped.getKey()) {
                    if (looped.getValue()) {
                        sendMessage(ev, "Looping single track",
                                "I'm going to loop `" + musicManager.player.getPlayingTrack().getInfo().title + "`!", Color.GREEN);
                    } else {
                        sendMessage(ev, "Looping playlist", "I'm going to loop this playlist upon completion", Color.GREEN);
                    }
                } else {
                    sendMessage(ev, "Not looping", "I'm not going to repeat tracks, that have already been played", Color.GREEN);
                }
            }
            case "volume" -> {
                int percent = musicManager.player.getVolume();
                int set = Integer.parseInt(args.get("volume"));
                if (set <= 0) {
                    sendMessage(ev, "I'm not going to play silently", "If you don't want to listen to a specific part of the track, please use `/jump [seconds]`", Color.RED);
                    return;
                } else if (set > 1000) {
                    sendMessage(ev, "That's way too loud", "If you want to blast your hears, I recommend 1000%, which is the maximum", Color.RED);
                    return;
                }
                musicManager.handler.volume(set);
                sendMessage(ev, "Volume modified",
                        String.join("", "The volume has been set from `", String.valueOf(percent), "%` to `", args.get("volume"), "%`"), Color.GREEN);
            }
            case "skip" -> {
                int skip = 1;
                if (args.get("amount") != null) skip = Integer.parseInt(args.get("amount"));
                if (musicManager.handler.skip(skip)) {
                    sendMessage(ev, String.join(" ", "Skipped", String.valueOf(skip), skip <= 1 ? "track" : "tracks"), null, Color.GREEN);
                } else {
                    sendMessage(ev, "Skipped to the end", "You skipped to the end and looping is disabled", Color.RED);
                }
            }
            case "jump" -> {
                int secs = Integer.parseInt(args.get("seconds"));
                String msg = secs < 0 ? "If you want to hear this track again, please use `/loop true`" : "If you want to play the next track, please use `/skip`";
                if (musicManager.handler.jump(secs)) {
                    sendMessage(ev, "Jumping...", "Skipped `" + secs + "s`", Color.GREEN);
                } else {
                    sendMessage(ev, "Song is not that long", msg, Color.RED);
                }
            }
            case "queue" -> {
                ev.deferReply(true).queue();
                List<AudioTrack> list = musicManager.handler.getQueue();
                EmbedBuilder embed = new EmbedBuilder().setTitle("Queue").setColor(Color.CYAN);
                embed.addField("Currently playing:", "**" + list.get(0).getInfo().title + "** by **" + list.get(0).getInfo().author + "**", false);
                for (int i = 1; i <= Math.min(20, list.size() - 1); i++) {

                    embed.addField("",
                            i + ".  " + (i >= 10 ? "" : " ") + "`" + list.get(i).getInfo().title + "` by `" + list.get(i).getInfo().author + "`", false);

                }
                if (list.size() > 21) {
                    embed.addField("", "*+" + (list.size() - 21) + " more...*", false);
                }
                ev.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
            }
            case "info" -> {
                AudioTrack track = musicManager.player.getPlayingTrack();
                AudioTrackInfo info = track.getInfo();

                long temp_duration = track.getDuration();
                int hours = (int) (temp_duration / 360000);
                temp_duration -= hours * 360000L;
                int mins = (int) (temp_duration / 60000);
                temp_duration -= mins * 60000L;
                int secs = (int) (temp_duration / 1000);

                temp_duration = track.getPosition();
                int poshours = (int) (temp_duration / 360000);
                temp_duration -= poshours * 360000L;
                int posmins = (int) (temp_duration / 60000);
                temp_duration -= posmins * 60000L;
                int possecs = (int) (temp_duration / 1000);

                String id = info.uri.replace("https://www.youtube.com/watch?v=","");
                boolean isYt = !id.equals(info.uri);

                ev.replyEmbeds(new EmbedBuilder().setTitle("Currently playing:")
                                .addField("Title", info.title, false)
                                .addField("Author", info.author, false)
                                .addField("Duration", "`" + ((hours > 0) ? (hours + "h ") : "") + (mins >= 10 ? "" : "0") + mins + "min " + (secs >= 10 ? "" : "0") + secs + "s`", false)
                                .addField("Current position: ", "`" + ((poshours > 0) ? (poshours + " : ") : "") + (posmins >= 10 ? "" : "0") + posmins + " : " + (possecs >= 10 ? "" : "0") + possecs + "`", false)
                        .setThumbnail(isYt ? "https://img.youtube.com/vi/" + id + "/hqdefault.jpg" : null).build()).setEphemeral(true).queue();
            }
            default -> sendMessage(ev, "Whoops", "I do not recognize this command, although I should ❓", Color.RED);
        }

    }

    private void sendMessage(SlashCommandInteractionEvent ev, String title, String description, Color color) {

        ev.replyEmbeds(new EmbedBuilder()
                .setTitle(title)
                .setColor(color)
                .setDescription(description).build()).setEphemeral(true).queue();

    }
}
