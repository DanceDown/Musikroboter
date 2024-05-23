package me.Musikroboter;

import dev.arbjerg.lavalink.client.player.Track;
import dev.arbjerg.lavalink.protocol.v4.TrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
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
                Musikroboter.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);

            }
            case "quit" -> quit(ev, g, m);
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
                        .addField("/play <Title/Link>", "Playing the given track(s)", false)
                        .addField("/pause {Duration}", "Takes a break", false)
                        .addField("/queue", "Shows the playlist", false)
                        .addField("/skip", "Skips the current track", false)
                        .addField("/volume <Percent>", "Regulates the volume", false)
                        .addField("/jump <Seconds>", "Skips the amount of seconds of the current track", false)
                        .addField("/shuffle", "Shuffles the playlist", false)
                        .addField("/loop {track}", "Repeats the current track or playlist", false)
                        .addField("/stop", "Stops playing", false)
                        .addField("/info", "Shows the detailed information about the current track", false)
                        .addField("/bass [Percent]", "Boosts the bass", false)
                        .addField("/pan [Speed]", "Rotate the sound around your ears", false)
                        .addField("/karaoke [Level] [Mono] [Band] [Width]", "Reduce vocals", false)
                        .addField("/speed <speed> [pitch] [rate]", "Speed up or slow down the track", false)
                        .addField("/help", "Shows this list", false)
                        .setDescription("If there's anything wrong with me or you have suggestions for improvement, please contact " + var).build())
                        .addActionRow(Button.success("support", "Support")).setEphemeral(true).queue();
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
                            .addActionRow(input).build();

                    ev.replyModal(modal).queue();
                }

            }
            case "stop" -> stop(ev, g);
            case "pause" -> pause(ev, g, args.get("duration"), args.get("timeunit"));
            case "shuffle" -> shuffle(ev, g);
            case "loop" -> loop(ev, g, args.get("track"));
            case "volume" -> {

                if(!args.containsKey("volume")) {
                    TextInput input = TextInput.create("volume", "Volume", TextInputStyle.SHORT)
                            .setPlaceholder("e.g. 50")
                            .setRequired(true)
                            .setMinLength(1)
                            .setMaxLength(3)
                            .build();

                    Modal modal = Modal.create("volume", "Volume Control")
                            .addActionRow(input).build();
                    ev.replyModal(modal).queue();
                } else volume(ev, g, args.get("volume"));

            }
            case "skip" -> {
                if(args.get("amount") != null) skip(ev, g, args.get("amount"));
                else {
                    TextInput input = TextInput.create("skip-num", "Amount", TextInputStyle.SHORT)
                            .setRequired(true)
                            .setValue("1")
                            .setPlaceholder("e.g. 1")
                            .setMinLength(1).build();
                    Modal modal = Modal.create("skip", "Skip Control")
                            .addActionRow(input).build();
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
                            .addActionRow(input).build();
                    ev.replyModal(modal).queue();
                } else jump(ev, g, args.get("seconds"));
            }
            case "queue" -> queue(ev, g);
            case "info" -> info(ev, g);
            case "bass" -> {
                if(!args.containsKey("amount")) {
                    TextInput input = TextInput.create("bass-num", "Bass Boost", TextInputStyle.SHORT)
                            .setRequired(true)
                            .setPlaceholder("in percent (Initially 0)")
                            .build();

                    Modal modal = Modal.create("bass", "Bass Control")
                            .addActionRow(input).build();
                    ev.replyModal(modal).queue();
                } else bass(ev, g, args.get("amount"));
            }
            case "pan" -> {
                if(!args.containsKey("speed")) {
                    TextInput input = TextInput.create("speed", "Speed", TextInputStyle.SHORT)
                            .setRequired(true)
                            .setPlaceholder("Frequency (100 = 1Hz)")
                            .build();

                    Modal modal = Modal.create("pan", "Panning")
                            .addActionRow(input).build();
                    ev.replyModal(modal).queue();
                } else pan(ev, g, args.get("speed"));
            }
            case "karaoke" -> {
                if(!(args.containsKey("level") || args.containsKey("mono") || args.containsKey("band") || args.containsKey("width"))) {
                    TextInput level = TextInput.create("level", "Level", TextInputStyle.SHORT)
                            .setRequired(false)
                            .setPlaceholder("0 - 100")
                            .build();
                    TextInput mono = TextInput.create("mono", "Mono", TextInputStyle.SHORT)
                            .setRequired(false)
                            .setPlaceholder("0 - 100")
                            .build();
                    TextInput band = TextInput.create("band", "Band", TextInputStyle.SHORT)
                            .setRequired(false)
                            .setPlaceholder("e.g. 250 (Hz)")
                            .build();
                    TextInput width = TextInput.create("width", "Width", TextInputStyle.SHORT)
                            .setRequired(false)
                            .setPlaceholder("e.g. 150 (Hz)")
                            .build();


                    Modal modal = Modal.create("karaoke", "Karaoke")
                            .addActionRow(level)
                            .addActionRow(mono)
                            .addActionRow(band)
                            .addActionRow(width).build();
                    ev.replyModal(modal).queue();

                } else karaoke(ev, g, args.get("level"), args.get("mono"), args.get("band"), args.get("width"));
            }
            case "speed" -> {
                if(!args.containsKey("speed")) {
                    TextInput speed = TextInput.create("speed", "Speed", TextInputStyle.SHORT)
                            .setRequired(true)
                            .setPlaceholder("e.g. 1.5")
                            .build();
                    TextInput pitch = TextInput.create("pitch", "Pitch", TextInputStyle.SHORT)
                            .setRequired(false)
                            .setPlaceholder("e.g. 2")
                            .build();
                    TextInput rate = TextInput.create("rate", "Rate", TextInputStyle.SHORT)
                            .setRequired(false)
                            .setPlaceholder("e.g. 10")
                            .build();


                    Modal modal = Modal.create("speed", "Speed")
                            .addActionRow(speed)
                            .addActionRow(pitch)
                            .addActionRow(rate).build();
                    ev.replyModal(modal).queue();

                } else timescale(ev, g, args.get("speed"), args.get("pitch"), args.get("rate"));
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

        switch(ev.getModalId()) {
            case "support" -> {
                String msg = getModalInput(ev.getValue("sup-msg"));
                User user = Musikroboter.getJda().getUserById(406780230645186561L);
                assert user != null;
                PrivateChannel channel = user.openPrivateChannel().complete();
                channel.sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.MAGENTA)
                        .setTitle("Support für " + ev.getGuild().getName())
                        .setDescription(ev.getUser().getAsMention() + " needs help!")
                        .addField("Following text was sent:", msg, false).build()).queue();
                sendMessage(ev, "Success!", "Your message has been sent and is currently reviewed", Color.GREEN);
            }
            case "title-ask" -> play(ev, g, m, getModalInput(ev.getValue("title")));
            case "volume" ->    volume(ev, g, getModalInput(ev.getValue("volume")));
            case "skip" ->      skip(ev, g, getModalInput(ev.getValue("skip-num")));
            case "jump" ->      jump(ev, g, getModalInput(ev.getValue("jump-num")));
            case "bass" ->      bass(ev, g, getModalInput(ev.getValue("bass-num")));
            case "pan" ->       pan(ev, g, getModalInput(ev.getValue("speed")));
            case "karaoke" ->   karaoke(ev, g, getModalInput(ev.getValue("level")),
                    getModalInput(ev.getValue("mono")), getModalInput(ev.getValue("band")), getModalInput(ev.getValue("width")));
            case "speed" -> timescale(ev, g, getModalInput(ev.getValue("speed")), getModalInput(ev.getValue("pitch")), getModalInput(ev.getValue("rate")));
        }

    }

    String getModalInput(ModalMapping value) {
        if(value == null)
            return null;
        return value.getAsString();
    }

    @Override
    public synchronized void onMessageReactionAdd(@NotNull MessageReactionAddEvent ev) {

        Guild g = ev.getGuild();
        Member m = ev.getMember();
        User u = ev.getUser();
        TrackHandler handler = PlayerManager.getINSTANCE().getTrackHandler(g);
        InfoCard ic = handler.getInfocard();

        if(ic.getMsg() == null) return;
        if(ev.getMessageIdLong() != ic.getMsg().getIdLong()) return;
        if(ev.getGuild().getSelfMember().equals(ev.getMember())) return;

        if(u == null) return;

        if(!ic.isPaused()) {
            if (ev.getEmoji().equals(InfoCard.pause)) pause(null, g, null, null);
            else if(ev.getEmoji().equals(InfoCard.loop)) loop(null, g, null);
            else if(ev.getEmoji().equals(InfoCard.loop1)) loop(null, g, Boolean.toString(!handler.isLooped().getValue()));
            else if(ev.getEmoji().equals(InfoCard.jump)) jump(null, g, "10");
            else if(ev.getEmoji().equals(InfoCard.shuffle)) shuffle(null, g);
            else if(ev.getEmoji().equals(InfoCard.skip)) skip(null, g, "1");
            else if(ev.getEmoji().equals(InfoCard.volume1)) volume(null, g, Integer.toString(handler.getPlayer().getVolume() - 5));
            else if(ev.getEmoji().equals(InfoCard.volume2)) volume(null, g, Integer.toString(handler.getPlayer().getVolume() + 5));
        }

        if(ev.getEmoji().equals(InfoCard.quit)) quit(null, g, m);
        else if(ev.getEmoji().equals(InfoCard.stop)) stop(null, g);
        else if(ev.getEmoji().equals(InfoCard.resume)) pause(null, g, null, null);
        else if(ev.getEmoji().equals(InfoCard.queue)) queue(null, g);

        if(ic.getMsg() != null) ic.getMsg().removeReaction(ev.getEmoji(), u).queue();

    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent ev) {

        InfoCard ic = PlayerManager.getINSTANCE().getTrackHandler(ev.getGuild()).getInfocard();

        if(ic.getMsg() == null) return;
        if(ev.getMessageIdLong() != ic.getMsg().getIdLong()) return;

        ic.reset();

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
                    .addActionRow(input).build();
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

    private void quit(GenericCommandInteractionEvent ev, Guild g, Member m) {
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
        PlayerManager.getINSTANCE().getTrackHandler(g).stop();
        g.getAudioManager().closeAudioConnection();
        Musikroboter.getJda().getPresence().setPresence(OnlineStatus.ONLINE, Activity.listening("/help"));
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

    private void stop(IReplyCallback ev, Guild g) {

        PlayerManager.getINSTANCE().getTrackHandler(g).stop();
        sendMessage(ev, "Stopping", "Would you like me to play something else instead?", Color.GREEN);
        Musikroboter.getJda().getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.listening("/help"));

    }

    private void pause(IReplyCallback ev, Guild g, String dur, String timeu) {

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

            boolean paused = PlayerManager.getINSTANCE().getTrackHandler(g).pause(duration, time);
            if (paused) {
                sendMessage(ev, "Pausing", String.join(" ", "The bot is going to pause in", String.valueOf(duration), time.name(), " ⏰"), Color.GREEN);
            } else {
                sendMessage(ev, "Continuing", String.join(" ", "The bot is going to continue playing in", String.valueOf(duration), time.name(), " ⏰"), Color.GREEN);
            }
        } else {
            boolean paused = PlayerManager.getINSTANCE().getTrackHandler(g).pause();
            if (paused) {
                sendMessage(ev, "Pausing","The bot is pausing", Color.GREEN);
            } else {
                sendMessage(ev, "Resuming", "The bot is resuming", Color.GREEN);
            }
        }
    }

    private void shuffle(IReplyCallback ev, Guild g) {

        PlayerManager.getINSTANCE().getTrackHandler(g).shuffle();
        sendMessage(ev, "Shuffling", "The list has successfully been shuffled", Color.GREEN);

    }

    private void loop(IReplyCallback ev, Guild g, String track) {

        TrackHandler handler = PlayerManager.getINSTANCE().getTrackHandler(g);
        Map.Entry<Boolean, Boolean> looped = handler.isLooped();
        if (track == null) {
            handler.setLooped(!looped.getKey(), looped.getValue());
        } else {
            handler.setLooped(true, Boolean.parseBoolean(track.toLowerCase()));
        }
        looped = handler.isLooped();
        if (looped.getKey()) {
            if (looped.getValue()) {
                Track tr = handler.getPlayer().getTrack();
                sendMessage(ev, "Looping single track",
                        "I'm going to loop `" + (tr == null ? "per track" : tr.getInfo().getTitle()) + "`!", Color.GREEN);
            } else {
                sendMessage(ev, "Looping playlist", "I'm going to loop this playlist!", Color.GREEN);
            }
        } else {
            sendMessage(ev, "Not looping", "I'm going to be quiet when the playlist ends!", Color.GREEN);
        }

    }

    private void volume(IReplyCallback ev, Guild g, String volume) {

        try {

            TrackHandler handler = PlayerManager.getINSTANCE().getTrackHandler(g);
            int percent = handler.getPlayer().getVolume();
            int set = Integer.parseInt(volume);
            if (set <= 0) {
                sendMessage(ev, "I'm not going to play silently", "If you don't want to listen to a specific part of the track, please use `/jump [seconds]`", Color.RED);
                return;
            } else if (set > 1000) {
                sendMessage(ev, "That's way too loud", "If you want to blast your hears, I recommend 1000%, which is the maximum", Color.RED);
                return;
            }
            handler.volume(set);
            sendMessage(ev, "Volume modified",
                    String.join("", "The volume has been set from `", String.valueOf(percent), "%` to `", volume, "%`"), Color.GREEN);

        } catch(NumberFormatException ex) {

            sendMessage(ev, "Invalid number", volume + " is not an integer number!", Color.RED);

        }
    }

    private void skip(IReplyCallback ev, Guild g, String amount) {

        try {

            int skip = Integer.parseInt(amount);
            if (PlayerManager.getINSTANCE().getTrackHandler(g).skip(skip)) {
                sendMessage(ev, String.join(" ", "Skipped", String.valueOf(skip), skip <= 1 ? "track" : "tracks"), null, Color.GREEN);
            } else {
                if (skip >= 1) sendMessage(ev, "Number not accepted", "The playlist isn't that big", Color.RED);
                else sendMessage(ev, "Number not accepted", "You can't skip backwards, sorry", Color.RED);
            }

        } catch(NumberFormatException ex) {

            sendMessage(ev, "Invalid number", amount + " is not an integer number!", Color.RED);

        }

    }

    private void jump(IReplyCallback ev, Guild g, String seconds) {

        try {
            int secs = Integer.parseInt(seconds);
            String msg = secs < 0 ? "If you want to hear this track again, please use `/loop true`" : "If you want to play the next track, please use `/skip`";
            if (PlayerManager.getINSTANCE().getTrackHandler(g).jump(secs)) {
                sendMessage(ev, "Jumping...", "Skipped `" + secs + "s`", Color.GREEN);
            } else {
                sendMessage(ev, "Song is not that long", msg, Color.RED);
            }
        } catch(NumberFormatException ex) {
            sendMessage(ev, "Invalid number", seconds + " is not an integer number!", Color.RED);
        }
    }

    private void queue(IReplyCallback ev, Guild g) {

        TrackHandler handler = PlayerManager.getINSTANCE().getTrackHandler(g);
        List<Track> list = handler.getQueue();
        EmbedBuilder embed = new EmbedBuilder().setTitle("Queue").setColor(Color.CYAN);
        embed.setDescription("Currently playing: **" + list.get(0).getInfo().getTitle() + "** by **" + list.get(0).getInfo().getAuthor() + "**");
        for (int i = 1; i <= Math.min(20, list.size() - 1); i++) {

            embed.appendDescription("\n" + i + ".  " + (i >= 10 ? "" : " ") + "`" + list.get(i).getInfo().getTitle() + "` by `" + list.get(i).getInfo().getAuthor() + "`");

        }
        if (list.size() > 21) {
            embed.appendDescription("\n*+ " + (list.size() - 21) + " more...*");
        }

        if(ev == null) {
            TextChannel channel = handler.getInfocard().getChannel();
            if(channel == null) {
                System.err.println("Suspicious behaviour: User reacted to InfoCard, but InfoCard is in no channel.");
                return;
            }
            channel.sendMessageEmbeds(embed.build()).queue(msg -> msg.delete().queueAfter(15L, TimeUnit.SECONDS));
        }
        else ev.replyEmbeds(embed.build()).setEphemeral(true).queue();

    }

    private void info(IReplyCallback ev, Guild g) {
        TrackHandler handler = PlayerManager.getINSTANCE().getTrackHandler(g);
        Track track = handler.getPlayer().getTrack();
        if(track == null) {
            sendMessage(ev, "Error", "Nothing playing right now!", Color.RED);
            return;
        }
        TrackInfo info = track.getInfo();

        long temp_duration = info.getLength();
        int hours = (int) (temp_duration / 3600000);
        temp_duration -= hours * 3600000L;
        int mins = (int) (temp_duration / 60000);
        temp_duration -= mins * 60000L;
        int secs = (int) (temp_duration / 1000);

        temp_duration = handler.getPlayer().getPosition();
        int poshours = (int) (temp_duration / 3600000);
        temp_duration -= poshours * 3600000L;
        int posmins = (int) (temp_duration / 60000);
        temp_duration -= posmins * 60000L;
        int possecs = (int) (temp_duration / 1000);

        ev.replyEmbeds(new EmbedBuilder().setTitle("Currently playing:")
                .setDescription("Title: `" + info.getTitle() + "`\n")
                .appendDescription("Author: `" + info.getAuthor() + "`\n")
                .appendDescription("Duration: `" + ((hours > 0) ? (hours + "h ") : "") + (mins >= 10 ? "" : "0") + mins + "min " + (secs >= 10 ? "" : "0") + secs + "s`\n")
                .appendDescription("Current position: `" + ((poshours > 0) ? (poshours + ":") : "") + (posmins >= 10 ? "" : "0") + posmins + ":" + (possecs >= 10 ? "" : "0") + possecs + "`")
                .setThumbnail(info.getArtworkUrl()).build()).setEphemeral(true).queue();
    }

    private void bass(IReplyCallback ev, Guild g, String amount) {

        try {
            int percentage = Integer.parseInt(amount);
            if (percentage < 0 || percentage > 1000) {
                sendMessage(ev, "Invalid number", "Only numbers between 0 and 1000 are accepted!", Color.RED);
                return;
            }
            PlayerManager.getINSTANCE().getTrackHandler(g).bassboost(percentage);
            sendMessage(ev, "Bass Boost", "Bass boost set to " + percentage + "%", Color.GREEN);

        }catch(NumberFormatException ex) {
            sendMessage(ev, "Invalid number", amount + " is not a valid number!", Color.RED);
        } catch(NullPointerException ex) {
            sendMessage(ev, "No input", "Bass boost amount needed!", Color.RED);
        }
    }

    private void pan(IReplyCallback ev, Guild g, String rate) {
        try {
          double speed = Double.parseDouble(rate);
          if(speed < 0 || speed > 1000) {
              sendMessage(ev, "Invalid number", "Only numbers between 0 and 1000 are accepted!", Color.RED);
              return;
          }

          PlayerManager.getINSTANCE().getTrackHandler(g).pan(speed/100.0);
          sendMessage(ev, "Panning", "Panning set to " + speed/100.0 + "Hz", Color.GREEN);

        } catch(NumberFormatException ex) {
            sendMessage(ev, "Invalid number", rate + " is not a valid number!", Color.RED);
        } catch(NullPointerException ex) {
            sendMessage(ev, "No input", "Panning speed needed!", Color.RED);
        }
    }

    private void karaoke(IReplyCallback ev, Guild g, String s_level, String s_mono, String s_band, String s_width) {
        try {

            int i_lvl = s_level == null || s_level.isBlank() ? 50 : Integer.parseInt(s_level);
            if(i_lvl < 0 || i_lvl > 100) {
                sendMessage(ev, "Invalid number", "Level only ranges between 0 and 100%", Color.RED);
                return;
            }
            int i_mono = s_mono == null || s_mono.isBlank() ? 100 : Integer.parseInt(s_mono);
            if(i_mono < 0 || i_mono > 100) {
                sendMessage(ev, "Invalid number", "Mono only ranges between 0 and 100%", Color.RED);
                return;
            }

            float band = s_band == null || s_band.isBlank() ? 150.0f : Float.parseFloat(s_band);
            float width = s_width == null || s_width.isBlank() ? 10.0f : Float.parseFloat(s_width);

            PlayerManager.getINSTANCE().getTrackHandler(g).karaoke(i_lvl / 100f, i_mono / 100f, band, width);
            sendMessage(ev, "Karaoke", "Karaoke set to " + i_lvl/100 + "%", Color.GREEN);

        } catch(NumberFormatException ex) {
            sendMessage(ev, "Invalid number", "Please enter valid numbers!", Color.RED);
        }  catch(NullPointerException ex) {
            sendMessage(ev, "Missing values", "Not enough information given!", Color.RED);
        }
    }

    private void timescale(IReplyCallback ev, Guild g, String s_speed, String s_pitch, String s_rate) {
        try {

            float speed = Float.parseFloat(s_speed);
            if(speed < 0 || speed > 100) {
                sendMessage(ev, "Invalid number", "Speed only ranges up to 100x", Color.RED);
                return;
            }

            float pitch = s_pitch == null || s_pitch.isBlank() ? 1.0f : Float.parseFloat(s_pitch);
            if(pitch < 0 || pitch > 100) {
                sendMessage(ev, "Invalid number", "Pitch can only be positive and only ranges up to 100", Color.RED);
                return;
            }

            float rate = s_rate == null || s_rate.isBlank() ? 1.0f : Float.parseFloat(s_rate);
            if(rate < 0 || rate > 100) {
                sendMessage(ev, "Invalid number", "Rate can only be positive and only ranges up to 100", Color.RED);
                return;
            }

            PlayerManager.getINSTANCE().getTrackHandler(g).timescale(speed, pitch, rate);
            sendMessage(ev, "Speed", "Speed set to " + speed + "x", Color.GREEN);

        } catch(NumberFormatException ex) {
            sendMessage(ev, "Invalid number", "Please enter valid numbers!", Color.RED);
        }  catch(NullPointerException ex) {
            sendMessage(ev, "Missing speed", "Speed needed!", Color.RED);
        }
    }

}
