package be.arcadeboard.customgamemenu;

import be.arcadeboard.api.ArcadeBoardPlugin;
import be.arcadeboard.api.annotations.GameName;
import be.arcadeboard.api.game.Game;
import be.arcadeboard.api.game.GameInformation;
import be.arcadeboard.api.game.GameOption;
import be.arcadeboard.api.game.events.GamePlayerJoinEvent;
import be.arcadeboard.api.game.events.GameStartEvent;
import be.arcadeboard.api.game.graphics.CharacterCanvas;
import be.arcadeboard.api.player.events.KeyDownEvent;
import be.arcadeboard.api.resources.ColorResource;
import be.arcadeboard.api.resources.ResourceFont;
import be.arcadeboard.api.resources.ResourcePack;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * By naming the game "GAME_MENU" you will override the internal
 * application with the same name.
 */
@GameName("GAME_MENU")
public class CustomGameMenu extends Game<CharacterCanvas> implements Game.KeyDownListener {
    // Make sure the resource pack is static so it only
    // needs to be constructed once (upon game load)
    private static ResourcePack resourcePack = null;

    // List of all games in the menu
    private List<GameInformation> gamesList = new ArrayList<GameInformation>();
    private GameInformation selectedGame = null;

    /**
     * Called when the game is first loaded and when a new instance
     * of the game is created.
     * <p>
     * By default, each game started by a player is a new instance. Meaning the scope
     * of your fields will only live for as long as this menu is open.
     *
     * @param plugin ArcadeBoard plugin instance
     */
    public CustomGameMenu(ArcadeBoardPlugin plugin) {
        super(plugin);

        // Application settings
        setOption(GameOption.TPS, 5);               // 5 ticks per second
        setOption(GameOption.SCREEN_HEIGHT, 15);    // 15 height
        setOption(GameOption.SCREEN_WIDTH, 25);     // 25 width (size of logo / 16px)
        setOption(GameOption.VISIBLE, false);       // DO NOT show the game in the menu (required for this menu app)

        // Do not use a black background
        setOption(GameOption.BACKGROUND, GameOption.Choice.DISABLED);

        // Construct the resource pack (logo of the server and custom font)
        try {
            if (resourcePack == null) {
                resourcePack = new ResourcePack();
                resourcePack.setVersion(1); // Good practice to increase version when changing the resource pack
                // The name of the font, image, icon does not need to be unique between multiple games
                // when using the static declared "resourcePack" in this class.
                // NOTE: Logo is copyrighted to Dyescape https://dyescape.com/
                resourcePack.addImage("LOGO", getClass().getResourceAsStream("/logo.png"));
            }
            // Never forget to add the created resource pack as a game option
            setOption(GameOption.RESOURCE_PACK, resourcePack);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * On game start
     *
     * @param event GameStartEvent
     */
    @Override
    public void onGameStart(GameStartEvent event) {
        // Register listeners
        addKeyDownListener(this);
    }

    /**
     * Programming loop (called 5 ticks per second)
     */
    public void loop() {
        // Get the main canvas
        CharacterCanvas canvas = getMainCanvas();

        // First clear the space of the menu items
        // The canvas is not reset on each game loop, this is the
        // responsibility of the game. That way you have more control
        // in choosing what parts need to be reset.
        // In this fill we are also cutting 1 line of the logo because the menu slightly overlays the logo
        canvas.fillRectangle(0, 9, 25, 5, ColorResource.TRANSPARENT);

        // Drag the logo
        canvas.setTitle(""); // No title
        canvas.drawImage(0, 0, resourcePack.getImageByName("LOGO"));    // The image is 400x160 meaning it is 25x10

        if (!gamesList.isEmpty()) {
            // Draw the menu items ( I want it to show max 5 items  )
            // The selected item should never go below the 3rd item (like a scrolling menu in iOS)
            // The top and bottom items should be darker
            int currentIdx = gamesList.indexOf(selectedGame);
            // The starting Y position slightly overlaps the logo, but this is ok
            int y = 9;

            int itemsBefore = currentIdx;
            int itemsAfter = gamesList.size() - 1 - itemsBefore;

            // First draw the top two items (if it has two top items)
            // Color them from dark_gray to gray
            for (int j = itemsBefore; j > 0 && j > (itemsBefore - 2); j--) {
                int idx = itemsBefore - j;
                drawMenuItem(canvas, y + 1 - idx, "&" + (7 + idx), gamesList.get(currentIdx - idx - 1));
            }

            // Draw the selected item
            drawMenuItem(canvas, y + 2, "&e", gamesList.get(currentIdx));

            // Lastly draw the bottom two items (if it has two bottom items)
            // Color them from gray to dark_gray
            for (int j = 0; j < itemsAfter && j < 2; j++) {
                drawMenuItem(canvas, y + 3 + j, "&" + (7 + j), gamesList.get(currentIdx + 1 + j));
            }
        } else {
            // Show an error
            canvas.writeString(4, 12, "&7No games available ...", ResourceFont.getDefaultFont());
        }
    }

    /**
     * Draw menu item
     *
     * @param canvas          Character canvas to draw on
     * @param y               Y position of the item
     * @param color           Color of item
     * @param gameInformation Game to draw as menu item
     */
    private void drawMenuItem(CharacterCanvas canvas, int y, String color, GameInformation gameInformation) {
        String gameName = gameInformation.getDisplayName().toUpperCase();
        int nameLength = gameName.length();
        // Make sure the X location results in a centered name
        int x = (25 - nameLength) / 2;
        canvas.writeString(x, y, color + gameName, ResourceFont.getDefaultFont());
    }

    /**
     * On player join
     *
     * @param event GamePlayerJoinEvent
     */
    @Override
    public void onPlayerJoin(GamePlayerJoinEvent event) {
        // Load all the games the player has permission to
        for (GameInformation gameInformation : getPlugin().getGameManager().getAvailableGames()) {
            // Check if the player has permission
            if (gameInformation.hasPermission(event.getGamePlayer().getPlayer())) {
                // Make sure to only show visible games
                if (gameInformation.getOptionBoolean(GameOption.VISIBLE)) {
                    gamesList.add(gameInformation);
                }
            }
        }

        // Set the default selected game to the first game
        if (!gamesList.isEmpty()) {
            selectedGame = gamesList.get(0);
        }
    }

    /**
     * On key down event
     *
     * @param event KeyDownEvent
     */
    public void onKeyDown(KeyDownEvent event) {
        if (gamesList.isEmpty()) {
            // No permissions to any games
            return;
        }

        int currentIdx = gamesList.indexOf(selectedGame);
        switch (event.getKey()) {
            case UP:
                // Move up the list
                if (currentIdx > 0) {
                    currentIdx--;
                    selectedGame = gamesList.get(currentIdx);
                }
                break;
            case DOWN:
                // Move down the list
                if (currentIdx < gamesList.size() - 1) {
                    currentIdx++;
                    selectedGame = gamesList.get(currentIdx);
                }
                break;
            case JUMP:
                // Start the selected game
                if (selectedGame != null) {
                    stop();
                    getPlugin().getGameManager().playGame(event.getGamePlayer(), selectedGame);
                }
                break;
            case SNEAK:             // Note pressing F also quits the menu (global key)
                // Quit menu/game
                stop();
                break;
            /*
            Scrolling: Because we have not specifically enabled scrolling,
            the NUM key is always cancelled when moving AWAY from NUM_9
            This allows us to use NUM 8 and NUM 1 to detect up or down scrolling
             */
            case NUM_1:
                // Move down the list
                if (currentIdx < gamesList.size() - 1) {
                    currentIdx++;
                    selectedGame = gamesList.get(currentIdx);
                }
                break;
            case NUM_8:
                // Move up the list
                if (currentIdx > 0) {
                    currentIdx--;
                    selectedGame = gamesList.get(currentIdx);
                }
                break;
        }
    }
}
