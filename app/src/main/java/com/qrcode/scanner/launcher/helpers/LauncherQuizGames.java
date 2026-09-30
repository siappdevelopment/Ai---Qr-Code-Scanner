package com.qrcode.scanner.launcher.helpers;

import androidx.annotation.NonNull;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.models.QuizGameItem;

public final class LauncherQuizGames {

    private static final QuizGameItem[] GAMES = {
            new QuizGameItem("Subway Surfers", "https://gamqora.com/topgames/Subway-Surfers/index.html", R.drawable.ic_qz_icon_1),
            new QuizGameItem("Roblox", "https://gamqora.com/topgames/Roblox/index.html", R.drawable.ic_qz_icon_2),
            new QuizGameItem("Candy Crush Saga", "https://gamqora.com/topgames/Candy-Crush-Saga/index.html", R.drawable.ic_qz_icon_3),
            new QuizGameItem("Ludo King®", "https://gamqora.com/topgames/Ludo-King/index.html", R.drawable.ic_qz_icon_4),
            new QuizGameItem("Temple Run 2", "https://gamqora.com/topgames/Temple-Run-2-Endless-Escape/index.html", R.drawable.ic_qz_icon_5),
            new QuizGameItem("Chess - Play and Learn Online", "https://gamqora.com/topgames/Chess-Play-and-Learn-Online/index.html", R.drawable.ic_qz_icon_6),
            new QuizGameItem("Tile Club", "https://gamqora.com/topgames/Tile-Club-Match-Puzzle-Games/index.html", R.drawable.ic_qz_icon_7),
            new QuizGameItem("Archery Battle 3D", "https://gamqora.com/topgames/Archery-Battle-3D/index.html", R.drawable.ic_qz_icon_8),
            new QuizGameItem("PUBG MOBILE", "https://gamqora.com/topgames/PUBG-MOBILE/index.html", R.drawable.ic_qz_icon_9),
            new QuizGameItem("Block Blast!", "http://gamqora.com/topgames/Block-Blast/index.html", R.drawable.ic_qz_icon_10),
            new QuizGameItem("Water Sort Puzzle", "https://gamqora.com/topgames/Water-Sort-Puzzle-Sort-Color/index.html", R.drawable.ic_qz_icon_11),
            new QuizGameItem("Hill Climb Racing", "https://gamqora.com/topgames/Hill-Climb-Racing/index.html", R.drawable.ic_qz_icon_12),
            new QuizGameItem("Worms Zone .io", "https://gamqora.com/topgames/Worms-Zone-io-Hungry-Snake/index.html", R.drawable.ic_qz_icon_13),
            new QuizGameItem("Angry Birds 2", "https://gamqora.com/topgames/Angry-Birds-2/index.html", R.drawable.ic_qz_icon_14),
            new QuizGameItem("Harry Potter Hogwarts Mystery", "https://gamqora.com/topgames/Harry-Potter-Hogwarts-Mystery/index.html", R.drawable.ic_qz_icon_15),
            new QuizGameItem("My Talking Angela", "https://gamqora.com/topgames/My-Talking-Angela/index.html", R.drawable.ic_qz_icon_16),
            new QuizGameItem("2 Player Games", "https://gamqora.com/topgames/2-Player-games-the-Challenge/index.html", R.drawable.ic_qz_icon_17),
            new QuizGameItem("Word Search Explorer", "https://gamqora.com/topgames/Word-Search-Explorer/index.html", R.drawable.ic_qz_icon_18),
            new QuizGameItem("Solitaire", "https://gamqora.com/topgames/Solitaire---Classic-Card-Game/index.html", R.drawable.ic_qz_icon_19),
            new QuizGameItem("Water Sort Puz", "https://gamqora.com/topgames/Water-Sort-Puz---Color-Game/index.html", R.drawable.ic_qz_icon_20),
            new QuizGameItem("Empires & Puzzles", "https://gamqora.com/topgames/Empires-Puzzles-Match-3-RPG/index.html", R.drawable.ic_qz_icon_21),
            new QuizGameItem("Snake.io", "https://gamqora.com/topgames/Snakeio-Fun-Snake-io-Games/index.html", R.drawable.ic_qz_icon_22),
            new QuizGameItem("Race Master 3D", "https://gamqora.com/topgames/Race-Master-3D-Car-Racing/index.html", R.drawable.ic_qz_icon_23),
            new QuizGameItem("Last War Survival Game", "https://gamqora.com/topgames/Last-WarSurvival-Game/index.html", R.drawable.ic_qz_icon_24),
            new QuizGameItem("Fishdom", "https://gamqora.com/topgames/Fishdom/index.html", R.drawable.ic_qz_icon_25),
            new QuizGameItem("Amaze GO!", "https://gamqora.com/topgames/Amaze-GO/index.html", R.drawable.ic_qz_icon_26),
            new QuizGameItem("Car Parking Multiplayer", "https://gamqora.com/topgames/Car-Parking-Multiplayer/index.html", R.drawable.ic_qz_icon_27),
            new QuizGameItem("8 Ball Pool", "https://gamqora.com/topgames/8-Ball-Pool/index.html", R.drawable.ic_qz_icon_28),
            new QuizGameItem("UNO!", "https://gamqora.com/topgames/UNOTM/index.html", R.drawable.ic_qz_icon_29),
            new QuizGameItem("Sudoku.com", "https://gamqora.com/topgames/Sudokucom-Classic-Sudoku/index.html", R.drawable.ic_qz_icon_30),
            new QuizGameItem("Sniper Battle", "https://gamqora.com/topgames/Sniper-Battle-3D-War-Shooter/index.html", R.drawable.ic_qz_icon_31),
            new QuizGameItem("Darts of Fury", "https://gamqora.com/topgames/Darts-of-Fury/index.html", R.drawable.ic_qz_icon_32),
            new QuizGameItem("Satisfying Toys", "https://gamqora.com/topgames/Satisfying-Toys-Calm-Games/index.html", R.drawable.ic_qz_icon_33),
            new QuizGameItem("My Talking Tom", "https://gamqora.com/topgames/My-Talking-Tom/index.html", R.drawable.ic_qz_icon_34),
            new QuizGameItem("Marvel Contest of Champions", "https://gamqora.com/topgames/Marvel-Contest-of-Champions/index.html", R.drawable.ic_qz_icon_35),
            new QuizGameItem("Stickman Party", "https://gamqora.com/topgames/Stickman-Party-234-MiniGames/index.html", R.drawable.ic_qz_icon_36),
            new QuizGameItem("Snake Clash!", "https://gamqora.com/topgames/Snake-Clash/index.html", R.drawable.ic_qz_icon_37),
            new QuizGameItem("Masha and the Bear Pizza Maker", "https://gamqora.com/topgames/Masha-and-the-Bear-Pizza-Maker/index.html", R.drawable.ic_qz_icon_38),
            new QuizGameItem("Monster Trucks Game for Kids 3", "https://gamqora.com/topgames/Monster-Trucks-Game-for-Kids-3/index.html", R.drawable.ic_qz_icon_39),
            new QuizGameItem("Match Factory!", "http://gamqora.com/topgames/Match-Factory/index.html", R.drawable.ic_qz_icon_40),
    };

    private LauncherQuizGames() {
    }

    public static int size() {
        return GAMES.length;
    }

    @NonNull
    public static QuizGameItem get(int index) {
        if (index < 0 || index >= GAMES.length) {
            throw new IndexOutOfBoundsException("Quiz game index out of range: " + index);
        }
        return GAMES[index];
    }
}

