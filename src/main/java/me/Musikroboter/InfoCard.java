package me.Musikroboter;

import dev.arbjerg.lavalink.client.player.Track;
import dev.arbjerg.lavalink.protocol.v4.TrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.awt.*;

public class InfoCard {

    public static final Emoji pause = Emoji.fromUnicode("⏸");
    public static final Emoji resume = Emoji.fromUnicode("▶");
    public static final Emoji stop = Emoji.fromUnicode("⏹");
    public static final Emoji quit = Emoji.fromUnicode("🚪");
    public static final Emoji skip = Emoji.fromUnicode("⏭");
    public static final Emoji jump = Emoji.fromUnicode("⏩");
    public static final Emoji loop = Emoji.fromUnicode("🔁");
    public static final Emoji loop1 = Emoji.fromUnicode("🔂");
    public static final Emoji volume1 = Emoji.fromUnicode("🔉");
    public static final Emoji volume2 = Emoji.fromUnicode("🔊");
    public static final Emoji shuffle = Emoji.fromUnicode("🔀");
    public static final Emoji queue = Emoji.fromUnicode("ℹ");

    private volatile Message msg;
    private volatile TextChannel channel;

    private boolean isPaused = false;

    // Deletes the message and sets msg to null
    public synchronized void destroy() {

        if(msg != null) {
            msg.delete().queue();
            msg = null;
        }

    }

    // Sets msg to null, but doesn't delete the message
    public synchronized void reset() {
        msg = null;
    }

    public synchronized void updateMessage(Track currentTrack, String nextTrack, TrackHandler handler) {

        if(isPaused) return;
        if(channel == null) return;
        if(currentTrack == null) {
            destroy();
            return;
        }

        long temp_duration = currentTrack.getInfo().getLength();
        int hours = (int) (temp_duration / 3600000);
        temp_duration -= hours * 3600000L;
        int mins = (int) (temp_duration / 60000);
        temp_duration -= mins * 60000L;
        int secs = (int) (temp_duration / 1000);

        TrackInfo info = currentTrack.getInfo();
        String id = info.getUri();
        if(info.getSourceName().equalsIgnoreCase("youtube")) {
            id = "https://img.youtube.com/vi/" + info.getIdentifier() + "/default.jpg";
        }

        MessageEmbed embed;
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.MAGENTA)
                .setTitle("Playing " + info.getTitle())
                .setDescription("Author: `" + info.getAuthor() + "`\n")
                .appendDescription("Duration: `" + (hours > 0 ? ((hours < 10 ? "0" : "") + hours + "h ") : "") + (mins < 10 ? "0" : "") + mins + "min " + (secs < 10 ? "0" : "") + secs + "s`\n")
                .appendDescription("Source: " + info.getUri() + "\n");

        if(handler.getKaraoke() || handler.getBass() || handler.getSpeed() || handler.getPanning()) {
            String modifiers = "Modifiers: ";
            if(handler.getBass()) modifiers += "`Bass Boosted`, ";
            if(handler.getKaraoke()) modifiers += "`Karaoke Mode`, ";
            if(handler.getSpeed()) modifiers += "`Speed/Pitched`, ";
            if(handler.getPanning()) modifiers += "`Panned`, ";
            builder.appendDescription(modifiers.substring(0, modifiers.length() - 2) + '\n');
        }

        builder.appendDescription("Next: `" + nextTrack + "`")
                .setThumbnail(id);

        embed = builder.build();

        updateMessage(embed);

    }

    private synchronized void updateMessage(MessageEmbed embed) {
        if(msg == null) {
            msg = channel.sendMessageEmbeds(embed).complete();
            msg.addReaction(pause).queue();
            msg.addReaction(jump).queue();
            msg.addReaction(skip).queue();
            msg.addReaction(loop).queue();
            msg.addReaction(loop1).queue();
            msg.addReaction(shuffle).queue();
            msg.addReaction(volume1).queue();
            msg.addReaction(volume2).queue();
            msg.addReaction(queue).queue();
            msg.addReaction(stop).queue();
            msg.addReaction(quit).queue();
        } else {
            try {
                msg.editMessageEmbeds(embed).queue();
            } catch (ErrorResponseException ex) {
                reset();
                updateMessage(embed);
            }
        }
    }

    public synchronized void setPaused(boolean paused) {

        if(msg == null) return;
        try {
            if (paused) {
                msg.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("Paused!")
                        .setDescription("The bot is currently taking a break!")
                        .setColor(Color.cyan).build()).queue(result -> msg.clearReactions().queue(result1 -> {
                    msg.addReaction(resume).queue();
                    msg.addReaction(queue).queue();
                    msg.addReaction(stop).queue();
                    msg.addReaction(quit).queue();
                }));

            } else {
                msg.clearReactions().queue(r -> {
                    msg.addReaction(pause).queue();
                    msg.addReaction(jump).queue();
                    msg.addReaction(skip).queue();
                    msg.addReaction(loop).queue();
                    msg.addReaction(loop1).queue();
                    msg.addReaction(shuffle).queue();
                    msg.addReaction(volume1).queue();
                    msg.addReaction(volume2).queue();
                    msg.addReaction(queue).queue();
                    msg.addReaction(stop).queue();
                    msg.addReaction(quit).queue();
                });
            }

            isPaused = paused;

        } catch(ErrorResponseException ex) {
            reset();
            setPaused(paused);
        }

    }

    public synchronized void setChannel(TextChannel channel) {
        this.channel = channel;
    }

    public synchronized Message getMsg() {
        return msg;
    }

    public synchronized boolean isPaused() {
        return isPaused;
    }

    public synchronized TextChannel getChannel() {
        return channel;
    }
}
