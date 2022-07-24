package me.Musikroboter;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

                if (activeVChannel != null && !m.getVoiceState().getChannel().equals(activeVChannel) && activeVChannel.getMembers().size() > 1) {
                    sendMessage(ev, "Sorry", "I can't do that whilst playing for somebody else! 💔", Color.RED);
                    return;
                }

                sendMessage(ev, "Greetings", "How can I help you? 😃", Color.GREEN);
                g.getAudioManager().openAudioConnection(activeVChannel = m.getVoiceState().getChannel());
                Musikroboter.jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);

            }
            case "quit" -> quit(ev, g, m, musicManager);
            case "help" -> {
                Member mem = g.getMemberById(406780230645186561L);
                String var;
                if(mem == null) var = User.fromId(406780230645186561L).getAsMention();
                else var = mem.getAsMention();
                ev.replyEmbeds(new EmbedBuilder()
                        .setColor(Color.MAGENTA)
                        .setTitle("Help")
                        .addField("/join", "Joining the channel", false)
                        .addField("/quit", "Leaving the channel", false)
                        .addField("/play [Title/Link]", "Playing the given track(s)", false)
                        .addField("/pause {Duration}", "Takes a break", false)
                        .addField("/queue", "Shows the playlist", false)
                        .addField("/skip", "Skips the current track", false)
                        .addField("/volume [Percent]", "Regulates the volume", false)
                        .addField("/jump [Seconds]", "Skips the amount of seconds of the current track", false)
                        .addField("/shuffle", "Shuffles the playlist", false)
                        .addField("/loop {track}", "Repeats the current track or playlist", false)
                        .addField("/stop", "Stops playing", false)
                        .addField("/info", "Shows the detailed information about the current track", false)
                        .addField("/bass [Percent]", "Boosts the bass", false)
                        .addField("/help", "Shows this list", false)
                        .setDescription("If there's anything wrong with me or you have suggestions for improvement, please contact " + var).build())
                        .addActionRows(ActionRow.of(Button.success("support", "Support"))).setEphemeral(true).queue();
            }
            case "play" -> {

                if(args.containsKey("title")) play(ev, g, m, args.get("title"));
                else {
                    TextInput input = TextInput.create("title", "Title or Link", TextInputStyle.SHORT)
                            .setMinLength(1)
                            .setRequired(true)
                            .setPlaceholder("e.g. https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                            .build();

                    Modal modal = Modal.create("title-ask", "Specify the track[s]:")
                            .addActionRows(ActionRow.of(input)).build();

                    ev.replyModal(modal).queue();
                }

            }
            case "stop" -> stop(ev, musicManager);
            case "pause" -> {

                if(!args.containsKey("duration")) {

                    TextInput when = TextInput.create("when", "When", TextInputStyle.SHORT)
                            .setRequired(false)
                            .setPlaceholder("e.g. 10").build();

                    TextInput unit = TextInput.create("unit", "Time unit", TextInputStyle.SHORT)
                            .setPlaceholder("e.g. seconds (short: s)")
                            .setRequired(false)
                            .build();

                    boolean paused = musicManager.player.isPaused();

                    Modal modal = Modal.create("pause", (paused ? "Resume" : "Pause") + " Control").addActionRows(ActionRow.of(when), ActionRow.of(unit)).build();
                    ev.replyModal(modal).queue();

                } else pause(ev, musicManager, args.get("duration"), args.get("timeunit"));

            }
            case "shuffle" -> shuffle(ev, musicManager);
            case "loop" -> loop(ev, musicManager, args.get("track"));
            case "volume" -> {

                if(!args.containsKey("volume")) {
                    TextInput input = TextInput.create("vol", "Volume", TextInputStyle.SHORT)
                            .setPlaceholder("e.g. 50")
                            .setRequired(true)
                            .setMinLength(1)
                            .setMaxLength(3)
                            .build();

                    Modal modal = Modal.create("volume", "Volume Control")
                            .addActionRows(ActionRow.of(input)).build();
                    ev.replyModal(modal).queue();
                } else volume(ev, musicManager, args.get("volume"));

            }
            case "skip" -> {
                if(args.get("amount") != null) skip(ev, musicManager, args.get("amount"));
                else {
                    TextInput input = TextInput.create("skip-num", "Amount", TextInputStyle.SHORT)
                            .setRequired(true)
                            .setValue("1")
                            .setPlaceholder("e.g. 1")
                            .setMinLength(1).build();
                    Modal modal = Modal.create("skip", "Skip Control")
                            .addActionRows(ActionRow.of(input)).build();
                    ev.replyModal(modal).queue();
                }
            }
            case "jump" -> {
                if(!args.containsKey("seconds")) {
                    TextInput input = TextInput.create("jump-num", "Amount", TextInputStyle.SHORT)
                            .setPlaceholder("in seconds")
                            .setRequired(true)
                            .build();
                    Modal modal = Modal.create("jump", "Jump Control")
                            .addActionRows(ActionRow.of(input)).build();
                    ev.replyModal(modal).queue();
                } else jump(ev, musicManager, args.get("seconds"));
            }
            case "queue" -> queue(ev, musicManager);
            case "info" -> info(ev, musicManager);
            case "bass" -> {
                if(!args.containsKey("amount")) {
                    TextInput input = TextInput.create("bass-num", "Bass Boost", TextInputStyle.SHORT)
                            .setRequired(true)
                            .setPlaceholder("in percent (Initially 0)")
                            .build();

                    Modal modal = Modal.create("bass", "Bass Control")
                            .addActionRows(ActionRow.of(input)).build();
                    ev.replyModal(modal).queue();
                } else bass(ev, musicManager, args.get("amount"));
            }
            default -> sendMessage(ev, "Whoops", "I do not recognize this command, although I should ❓", Color.RED);
        }

    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent ev) {

        Member m = ev.getMember();
        Guild g;
        if((g = ev.getGuild()) == null) {
            sendMessage(ev, "Error", "Couldn't fetch the guild", Color.RED);
            return;
        }

        MusicManager musicManager = PlayerManager.getINSTANCE().getMusicManager(g);

        switch(ev.getModalId()) {
            case "support" -> {
                String msg = ev.getValue("sup-msg").getAsString();
                User user = Musikroboter.jda.getUserById(406780230645186561L);
                assert user != null;
                PrivateChannel channel = user.openPrivateChannel().complete();
                channel.sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.MAGENTA)
                        .setTitle("Support für " + ev.getGuild().getName())
                        .setDescription(ev.getUser().getAsTag() + " needs help!")
                        .addField("Following text was sent:", msg, false).build()).queue();
                sendMessage(ev, "Success!", "Your message has been sent and is currently reviewed", Color.GREEN);
            }
            case "title-ask" -> play(ev, g, m, ev.getValue("title").getAsString());
            case "pause" -> pause(ev, musicManager, ev.getValue("when") == null ? null : ev.getValue("when").getAsString(),
                    ev.getValue("unit") == null ? null : ev.getValue("unit").getAsString());
            case "volume" -> volume(ev, musicManager, ev.getValue("vol").getAsString());
            case "skip" -> skip(ev, musicManager, ev.getValue("skip-num").getAsString());
            case "jump" -> jump(ev, musicManager, ev.getValue("jump-num").getAsString());
            case "bass" -> bass(ev, musicManager, ev.getValue("bass-num").getAsString());
        }

    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent ev) {

        if(InfoCard.getMsg() == null) return;
        if(ev.getMessageIdLong() != InfoCard.getMsg().getIdLong()) return;
        if(ev.getGuild().getSelfMember().equals(ev.getMember())) return;

        Guild g = ev.getGuild();
        Member m = ev.getMember();
        User u = ev.getUser();

        if(u == null) return;

        MusicManager musicManager = PlayerManager.getINSTANCE().getMusicManager(g);

        if(!InfoCard.isPaused()) {
            if (ev.getEmoji().equals(InfoCard.pause)) pause(null, musicManager, null, null);
            else if(ev.getEmoji().equals(InfoCard.loop)) loop(null, musicManager, null);
            else if(ev.getEmoji().equals(InfoCard.loop1)) loop(null, musicManager, Boolean.toString(!musicManager.handler.isLooped().getValue()));
            else if(ev.getEmoji().equals(InfoCard.jump)) jump(null, musicManager, "10");
            else if(ev.getEmoji().equals(InfoCard.shuffle)) shuffle(null, musicManager);
            else if(ev.getEmoji().equals(InfoCard.skip)) skip(null, musicManager, "1");
            else if(ev.getEmoji().equals(InfoCard.bass1)) bass(null, musicManager, Integer.toString(musicManager.bassboost - 5));
            else if(ev.getEmoji().equals(InfoCard.bass2)) bass(null, musicManager, Integer.toString(musicManager.bassboost + 5));
            else if(ev.getEmoji().equals(InfoCard.volume1)) volume(null, musicManager, Integer.toString(musicManager.player.getVolume() - 5));
            else if(ev.getEmoji().equals(InfoCard.volume2)) volume(null, musicManager, Integer.toString(musicManager.player.getVolume() + 5));
        }
        if(ev.getEmoji().equals(InfoCard.quit)) quit(null, g, m, musicManager);
        else if(ev.getEmoji().equals(InfoCard.stop)) stop(null, musicManager);
        else if(ev.getEmoji().equals(InfoCard.resume)) pause(null, musicManager, null, null);
        else if(ev.getEmoji().equals(InfoCard.queue)) queue(null, musicManager);

        if(InfoCard.getMsg() != null) InfoCard.getMsg().removeReaction(ev.getEmoji(), u).queue();

    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent ev) {

        if(InfoCard.getMsg() == null) return;
        if(ev.getMessageIdLong() != InfoCard.getMsg().getIdLong()) return;
        InfoCard.reset();

    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent ev) {

        String id;
        if((id = ev.getButton().getId()) == null) return;
        if ("support".equals(id)) {
            TextInput input = TextInput.create("sup-msg", "Message", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("e.g. why is the bot not playing?")
                    .setRequired(true)
                    .setMinLength(10).build();
            Modal modal = Modal.create("support", "Support")
                    .addActionRows(ActionRow.of(input)).build();
            ev.replyModal(modal).queue();
        }

    }

    private void sendMessage(IReplyCallback ev, String title, String description, Color color) {

        if(ev == null) return;
        ev.replyEmbeds(new EmbedBuilder()
                .setTitle(title)
                .setColor(color)
                .setDescription(description).build()).setEphemeral(true).queue();

    }

    private void quit(GenericCommandInteractionEvent ev, Guild g, Member m, MusicManager musicManager) {
        if (activeVChannel == null) {
            sendMessage(ev, "Jokes on you", "I'm currently in no channel! 👊", Color.RED);
            return;
        }
        if (m.getVoiceState() != null && m.getVoiceState().getChannel() != null && !m.getVoiceState().getChannel().equals(activeVChannel) && activeVChannel.getMembers().size() > 1) {
            sendMessage(ev, "Sorry", "I can't do that whilst playing for somebody else! 💔", Color.RED);
            return;
        }

        sendMessage(ev, "Goodbye", "Have a great day! 👋", Color.MAGENTA);
        activeVChannel = null;
        musicManager.handler.stop();
        g.getAudioManager().closeAudioConnection();
        Musikroboter.jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.listening("/help"));

    }

    private void play(IReplyCallback ev, Guild g, Member m, String arg) {

        if(activeVChannel == null) {
            if(m.getVoiceState() == null || m.getVoiceState().getChannel() == null) {
                sendMessage(ev, "Where?", "You are currently in no channel 🙃", Color.RED);
                return;
            } else g.getAudioManager().openAudioConnection(activeVChannel = m.getVoiceState().getChannel());
        }
        if (m.getVoiceState() != null && m.getVoiceState().getChannel() != null && !m.getVoiceState().getChannel().equals(activeVChannel) && activeVChannel.getMembers().size() > 1) {
            sendMessage(ev, "Sorry", "I can't do that whilst playing for somebody else! 💔", Color.RED);
            return;
        }

        PlayerManager.getINSTANCE().load((TextChannel) ev.getMessageChannel(), ev, arg);
    }

    private void stop(IReplyCallback ev, MusicManager musicManager) {

        musicManager.handler.stop();
        sendMessage(ev, "Stopping", "Would you like me to play something else instead?", Color.GREEN);
        Musikroboter.jda.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.listening("/help"));

    }

    private void pause(IReplyCallback ev, MusicManager musicManager, String dur, String timeu) {

        int duration;

        if(dur != null) {
            try {
                duration = Integer.parseInt(dur);
                if (duration <= 0) sendMessage(ev, "Duration invalid!", "Please pick above zero!", Color.red);
            } catch (NumberFormatException ex) {
                sendMessage(ev, "Duration invalid!", "`" + dur + "` is not a valid integer number!", Color.RED);
                return;
            }

            TimeUnit time;
            String timeunit;
            if ((timeunit = timeu) != null) {
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
            if (paused) {
                sendMessage(ev, "Pausing","The bot is pausing", Color.GREEN);
            } else {
                sendMessage(ev, "Resuming", "The bot is resuming", Color.GREEN);
            }
        }
    }

    private void shuffle(IReplyCallback ev, MusicManager musicManager) {

        musicManager.handler.shuffle();
        sendMessage(ev, "Shuffling", "The list has successfully been shuffled", Color.GREEN);

    }

    private void loop(IReplyCallback ev, MusicManager musicManager, String track) {

        Map.Entry<Boolean, Boolean> looped = musicManager.handler.isLooped();
        if (track == null) {
            musicManager.handler.setLooped(!looped.getKey(), looped.getValue());
        } else {
            musicManager.handler.setLooped(true, Boolean.parseBoolean(track.toLowerCase()));
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

    private void volume(IReplyCallback ev, MusicManager musicManager, String volume) {

        try {

            int percent = musicManager.player.getVolume();
            int set = Integer.parseInt(volume);
            if (set <= 0) {
                sendMessage(ev, "I'm not going to play silently", "If you don't want to listen to a specific part of the track, please use `/jump [seconds]`", Color.RED);
                return;
            } else if (set > 1000) {
                sendMessage(ev, "That's way too loud", "If you want to blast your hears, I recommend 1000%, which is the maximum", Color.RED);
                return;
            }
            musicManager.handler.volume(set);
            sendMessage(ev, "Volume modified",
                    String.join("", "The volume has been set from `", String.valueOf(percent), "%` to `", volume, "%`"), Color.GREEN);

        } catch(NumberFormatException ex) {

            sendMessage(ev, "Invalid number", volume + " is not an integer number!", Color.RED);

        }
    }

    private void skip(IReplyCallback ev, MusicManager musicManager, String amount) {

        try {

            int skip = Integer.parseInt(amount);
            if (musicManager.handler.skip(skip)) {
                sendMessage(ev, String.join(" ", "Skipped", String.valueOf(skip), skip <= 1 ? "track" : "tracks"), null, Color.GREEN);
            } else {
                if (skip >= 1) sendMessage(ev, "Number not accepted", "The playlist isn't that big", Color.RED);
                else sendMessage(ev, "Number not accepted", "You can't skip backwards, sorry", Color.RED);
            }

        } catch(NumberFormatException ex) {

            sendMessage(ev, "Invalid number", amount + " is not an integer number!", Color.RED);

        }

    }

    private void jump(IReplyCallback ev, MusicManager musicManager, String seconds) {

        try {
            int secs = Integer.parseInt(seconds);
            String msg = secs < 0 ? "If you want to hear this track again, please use `/loop true`" : "If you want to play the next track, please use `/skip`";
            if (musicManager.handler.jump(secs)) {
                sendMessage(ev, "Jumping...", "Skipped `" + secs + "s`", Color.GREEN);
            } else {
                sendMessage(ev, "Song is not that long", msg, Color.RED);
            }
        } catch(NumberFormatException ex) {
            sendMessage(ev, "Invalid number", seconds + " is not an integer number!", Color.RED);
        }
    }

    private void queue(IReplyCallback ev, MusicManager musicManager) {

        List<AudioTrack> list = musicManager.handler.getQueue();
        EmbedBuilder embed = new EmbedBuilder().setTitle("Queue").setColor(Color.CYAN);
        embed.setDescription("Currently playing: **" + list.get(0).getInfo().title + "** by **" + list.get(0).getInfo().author + "**");
        for (int i = 1; i <= Math.min(20, list.size() - 1); i++) {

            embed.appendDescription("\n" + i + ".  " + (i >= 10 ? "" : " ") + "`" + list.get(i).getInfo().title + "` by `" + list.get(i).getInfo().author + "`");

        }
        if (list.size() > 21) {
            embed.appendDescription("\n*+ " + (list.size() - 21) + " more...*");
        }

        if(ev == null) InfoCard.getChannel().sendMessageEmbeds(embed.build()).queue(msg -> msg.delete().queueAfter(15L, TimeUnit.SECONDS));
        else ev.replyEmbeds(embed.build()).setEphemeral(true).queue();

    }

    private void info(IReplyCallback ev, MusicManager musicManager) {
        AudioTrack track = musicManager.player.getPlayingTrack();
        AudioTrackInfo info = track.getInfo();

        long temp_duration = track.getDuration();
        int hours = (int) (temp_duration / 3600000);
        temp_duration -= hours * 3600000L;
        int mins = (int) (temp_duration / 60000);
        temp_duration -= mins * 60000L;
        int secs = (int) (temp_duration / 1000);

        temp_duration = track.getPosition();
        int poshours = (int) (temp_duration / 3600000);
        temp_duration -= poshours * 3600000L;
        int posmins = (int) (temp_duration / 60000);
        temp_duration -= posmins * 60000L;
        int possecs = (int) (temp_duration / 1000);

        String id = info.uri.replace("https://www.youtube.com/watch?v=","");
        boolean isYt = !id.equals(info.uri);

        ev.replyEmbeds(new EmbedBuilder().setTitle("Currently playing:")
                .setDescription("Title: `" + info.title + "`\n")
                .appendDescription("Author: `" + info.author + "`\n")
                .appendDescription("Duration: `" + ((hours > 0) ? (hours + "h ") : "") + (mins >= 10 ? "" : "0") + mins + "min " + (secs >= 10 ? "" : "0") + secs + "s`\n")
                .appendDescription("Current position: `" + ((poshours > 0) ? (poshours + " : ") : "") + (posmins >= 10 ? "" : "0") + posmins + " : " + (possecs >= 10 ? "" : "0") + possecs + "`")
                .setThumbnail(isYt ? "https://img.youtube.com/vi/" + id + "/hqdefault.jpg" : null).build()).setEphemeral(true).queue();
    }

    private void bass(IReplyCallback ev, MusicManager musicManager, String amount) {

        try {

            int percentage = Integer.parseInt(amount);
            if (Math.abs(percentage - 250) > 250) {
                sendMessage(ev, "Invalid number", "Only numbers between 0 and 500 are accepted!", Color.RED);
                return;
            }
            int from = musicManager.bassboost(percentage);
            sendMessage(ev, "Bass Boost", "Set the bass boost from " + from + " to " + percentage, Color.GREEN);

        }catch(NumberFormatException ex) {
            sendMessage(ev, "Invalid number", amount + " is not a valid number!", Color.RED);
        }
    }

}
