package dev.javis.javigg.livegame;

public class NotInGameException extends RuntimeException {

    public NotInGameException() {
        super("Player is not currently in a game");
    }
}
