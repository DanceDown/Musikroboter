package me.Musikroboter;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

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
    public static final Emoji bass1 = Emoji.fromUnicode("📉");
    public static final Emoji bass2 = Emoji.fromUnicode("📈");
    public static final Emoji shuffle = Emoji.fromUnicode("🔀");
    public static final Emoji queue = Emoji.fromUnicode("ℹ");

    private static Message msg;
    private static TextChannel channel;

    private static boolean isPaused = false;

    // Deletes the message and sets msg to null
    public static void destroy() {

        if(msg != null) {
            msg.delete().queue();
            msg = null;
        }

    }

    // Sets msg to null, but doesn't delete the message
    public static void reset() {
        msg = null;
    }

    public static void updateMessage(AudioTrack currentTrack, String nextTrack) {

        long temp_duration = currentTrack.getDuration();
        int hours = (int) (temp_duration / 3600000);
        temp_duration -= hours * 3600000L;
        int mins = (int) (temp_duration / 60000);
        temp_duration -= mins * 60000L;
        int secs = (int) (temp_duration / 1000);

        AudioTrackInfo info = currentTrack.getInfo();
        String id = info.uri.replace("https://www.youtube.com/watch?v=","");
        boolean isYt = !id.equals(info.uri);

        MessageEmbed embed = new EmbedBuilder()
                .setColor(Color.MAGENTA)
                .setTitle("Playing " + info.title)
                .setDescription("Author: `" + info.author + "`\n")
                .appendDescription("Duration: `" + (hours > 0 ? ((hours < 10 ? "0" : "") + hours + "h ") : "") + (mins < 10 ? "0" : "") + mins + "min " + (secs < 10 ? "0" : "") + secs + "s`\n")
                .appendDescription("Source: " + info.uri + "\n")
                .appendDescription("Next: `" + nextTrack + "`")
                .setThumbnail((isYt ? "https://img.youtube.com/vi/" + id + "/hqdefault.jpg" : null)).build();

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
            msg.addReaction(bass1).queue();
            msg.addReaction(bass2).queue();
            msg.addReaction(queue).queue();
            msg.addReaction(stop).queue();
            msg.addReaction(quit).queue();
        }
        else msg.editMessageEmbeds(embed).queue();

        isPaused = false;

    }

    public static void setPaused(boolean paused) {

        if(paused) {

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
                msg.addReaction(bass1).queue();
                msg.addReaction(bass2).queue();
                msg.addReaction(queue).queue();
                msg.addReaction(stop).queue();
                msg.addReaction(quit).queue();
            });

        }

        isPaused = paused;

    }

    public static void setChannel(TextChannel channel) {
        InfoCard.channel = channel;
    }

    public static Message getMsg() {
        return msg;
    }

    public static boolean isPaused() {
        return isPaused;
    }

    public static TextChannel getChannel() {
        return channel;
    }
}
