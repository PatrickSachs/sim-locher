package de.wolc.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;

import de.wolc.MultiUse;
import de.wolc.gui.PapierObjekt;
import de.wolc.spiel.Farbe;
import de.wolc.spiel.Spieler;
import de.wolc.spiel.locher.Lochprozess;
import de.wolc.spiel.locher.upgrades.LocherUpgrade;
import de.wolc.spiel.papier.A4;
import de.wolc.spiel.papier.A5;
import de.wolc.spiel.papier.A6;
import de.wolc.spiel.papier.Konfetti;
import de.wolc.spiel.papier.Papier;
import de.wolc.spiel.papier.PapierStapel;
import de.wolc.gui.LocherPapierObjekt;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
import javafx.animation.AnimationTimer;
import javafx.scene.paint.ImagePattern;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.media.AudioClip;
import javafx.scene.control.TextInputDialog;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class Game extends AnimationTimer {

    private final String windowTitle = "World of Locher Craft";
    private Spieler spieler;
    private Rectangle locher_new;
    private AnchorPane gameArea;   
    private Stage stage;
    
    private static final Random RANDOM = new Random();
    private static final double BENACHRICHTUNG_ANZEIGEZEIT = 3.5d;
    private static final double ZIEL_FPS = 30d;
    
    //Game Variables
    private static final double STANDARD_REMAINING_TIME_AVAILABLE = 30d;
    private double remainingTimeAvailable;
    private double benachrichtigungenZeit = 0d;
    private ArrayList<LocherUpgrade> upgrades;

    //Variables for Countdown timer
    private double deltaZeit = 0d;
    private long letzteNanoZeit = 0;
    private double timeToNextPapier = 0d;
    private double fps = 0d;

    //Papierstapel erstellen
    private PapierStapel<A4> stapel_A4;
    private PapierStapel<A5> stapel_A5;
    private PapierStapel<A6> stapel_A6;

    //Hintergrundmusik
    private Media hintergrundMusikMedia;
    private MediaPlayer hintergrundMusik;

    //Diverse Nodes
    private Label fpsLabel, score, remainingTime, formatLabel, papierLabel, locherCooldown, benachrichtigungen;
    private ToggleButton formatA4Button, formatA5Button, formatA6Button;
    private HashMap<Farbe, Label> scoreLabels = new HashMap<>();

    private ArrayList<LocherPapierObjekt> locherPapier= new ArrayList<LocherPapierObjekt>();
    private ArrayList<KonfettiObjekt> konfettiObjekte = new ArrayList<>();

    public Game () {
        try {
            this.spieler = (Spieler) Gui.DB.laden("spieler");
        } catch (Exception e) {
            Alert ladeFehler = new Alert(AlertType.INFORMATION);
            ladeFehler.setTitle("Fehler bei Spielstand laden");
            ladeFehler.setHeaderText("Beim Laden des Spielstandes ist ein Fehler aufgetreten. Dies liegt " +
                "wahrscheinlich daran, dass eine neue Version des Spiels nicht mit der vorherigen kompatibel " +
                "ist.\nEs wurde deshalb ein neuer Spielstand begonnen.");
            ladeFehler.setContentText(e.toString());
            ladeFehler.setResult(ButtonType.OK);
            ladeFehler.showAndWait();
            e.printStackTrace();

            this.spieler = null;
        }
        if (this.spieler == null) {
            this.spieler = new Spieler();
            TextInputDialog dialog = new TextInputDialog(this.spieler.getName());
            dialog.setTitle("Spielername");
            dialog.setHeaderText("Gebe deinen Spielernamen ein.");
            dialog.setContentText("Name:");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                this.spieler.setName(result.get());
            }
        }
        this.letzteNanoZeit = 0;

        upgrades = this.spieler.getLocher().getUpgrades();
        remainingTimeAvailable = STANDARD_REMAINING_TIME_AVAILABLE;
        for (LocherUpgrade upgrade : upgrades) {
            remainingTimeAvailable = upgrade.upgradeSpielZeit(remainingTimeAvailable); 
        }
        
    }



    private void updateLabels() {
        // Score einteilen nach Farbe
        HashMap<Farbe, ArrayList<Konfetti>> hash = this.spieler.getKonfettiSortiert();
        for(Farbe farbe : Farbe.values()) {
            Label label = this.scoreLabels.get(farbe);
            ArrayList<Konfetti> liste = hash.get(farbe);
            int zahl = liste != null ? liste.size() : 0;
            label.setText("  " + farbe.getAnzeigeName() + ": " + zahl);
        }
        this.fpsLabel.setText("FPS: " + (Math.round(this.fps * 10d) / 10d));
        this.score.setText("Score: " + this.spieler.getKonfetti().size());
        // Sonstige Stats
        this.formatLabel.setText("Format: " + this.spieler.getLocher().getFormat().getSimpleName());
        this.papierLabel.setText("Stapel: " + this.spieler.getLocher().getStapel().groesse() + "/" + this.spieler.getLocher().getStaerke());
        this.formatLabel.setText("Stanzer: " + this.spieler.getLocher().getStanzer());
        this.locherCooldown.setText("Cooldown: " + Math.round(spieler.getLocher().getCooldown() * 10d) / 10d + "s");
        remainingTime.setText("Zeit: " + Math.round(this.remainingTimeAvailable * 10d) / 10d + "s");   
    }
    
    public Scene GameMainStage(Stage stage){
        this.stage = stage;

        //Hintergrundmusik
        if(Gui.getEinstellungen().ambientSoundEnabled()){
            hintergrundMusikMedia = new Media(MultiUse.url("de/wolc/gui/sounds/" + spieler.geHintergrundMusik().getMusikName()));
            hintergrundMusik = new MediaPlayer(hintergrundMusikMedia);
            hintergrundMusik.setCycleCount(MediaPlayer.INDEFINITE);
            hintergrundMusik.setVolume(Gui.getEinstellungen().getAmbientSoundVolume());
            hintergrundMusik.setAutoPlay(true);
            hintergrundMusik.setOnEndOfMedia(new Runnable(){
            
                @Override
                public void run() {
                    hintergrundMusik.seek(Duration.INDEFINITE);
                    hintergrundMusik.play();
                }
            });
        }


        //Main Orientation Node and initale settings
        BorderPane mainPane = new BorderPane();

        //Setting and creating the new Scene
        Scene gameScene = new Scene(mainPane);

        //Set Fullscreen
        //TODO: wenn man den Fullscreen verlässt skalieren die Nodes nicht mehr nach bzw. ändern Ihre Position nicht erneut
        this.stage.setFullScreen(Gui.getEinstellungen().isVollbild());
        this.stage.setFullScreenExitHint("");

        //Setting Background and width and height to screen
        MultiUse mu = new MultiUse();
        int[] windowSize = mu.GetScreenSize();
        Image backgroundImage = new Image("de/wolc/gui/images/" + this.spieler.getSchreibtischSkin().getGuiBild(), (double)windowSize[0], (double)windowSize[1], false, false);

        BackgroundImage backgroundImageGame = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,BackgroundSize.DEFAULT);
        mainPane.setBackground(new Background(backgroundImageGame));

        //Papierstapel creation
        stapel_A4 = new PapierStapel<>(A4.class);
        stapel_A5 = new PapierStapel<>(A5.class);
        stapel_A6 = new PapierStapel<>(A6.class);
        if(this.spieler.getLocher().getStapel() == null){
            this.spieler.getLocher().setFormat(A4.class);
            this.spieler.getLocher().einlegen(stapel_A4);
        }
        
        //Creating the Component-nodes
        //Creating the VBox for the right Output
        VBox rightVBox = new VBox();

        // Labels erstellen
        score = new Label();
        papierLabel = new Label();
        formatLabel = new Label();
        remainingTime = new Label();
        locherCooldown = new Label();
        this.fpsLabel = new Label();
        locherCooldown.setTextFill(Color.WHITE);
        papierLabel.setTextFill(Color.WHITE);
        score.setTextFill(Color.WHITE);
        remainingTime.setTextFill(Color.WHITE);
        formatLabel.setTextFill(Color.WHITE);
        this.fpsLabel.setTextFill(Color.WHITE);
        rightVBox.getChildren().addAll(fpsLabel, remainingTime, score);
        for(Farbe farbe : Farbe.values()) {
            Label label = new Label();
            label.setTextFill(farbe.getGuiFarbe());
            this.scoreLabels.put(farbe, label);
            rightVBox.getChildren().add(label);
        }
        rightVBox.getChildren().addAll(locherCooldown, formatLabel, papierLabel);
        this.updateLabels();

        //Adding the Format ToggleButtons + ToggleGroup + default ToggleButton configuration
        HBox formatBox = new HBox();
        ToggleGroup formatGroup = new ToggleGroup();
        formatBox.setPadding(new Insets(20, 5 , 20 ,5));

        formatA4Button = new ToggleButton("A4");
        formatA4Button.setSelected(true);
        formatA4Button.setToggleGroup(formatGroup);
        formatA4Button.setOnMousePressed((MouseEvent e) -> {
            papierWechsel(stapel_A4, A4.class);
            locherPapierEntfernen();
        });

        formatA5Button = new ToggleButton("A5");
        formatA5Button.setSelected(false);
        formatA5Button.setToggleGroup(formatGroup);
        formatA5Button.setOnMousePressed((MouseEvent e) -> {
            papierWechsel(stapel_A5, A5.class);
            locherPapierEntfernen();
        });

        formatA6Button = new ToggleButton("A6");
        formatA6Button.setSelected(false);
        formatA6Button.setToggleGroup(formatGroup);
        formatA6Button.setOnMousePressed((MouseEvent e) -> {
            papierWechsel(stapel_A6, A6.class);
            locherPapierEntfernen();
        });

        formatBox.getChildren().addAll(formatA4Button, formatA5Button, formatA6Button);
        mainPane.setLeft(formatBox);
        BorderPane.setAlignment(formatBox, Pos.CENTER_LEFT);

        //If a SaveGame has been loaded the ToggleButtons get adjusted here
        this.formatA4Button.setSelected(this.spieler.getLocher().getFormat() == A4.class);
        this.formatA5Button.setSelected(this.spieler.getLocher().getFormat() == A5.class);
        this.formatA6Button.setSelected(this.spieler.getLocher().getFormat() == A6.class);

        //Adding remainingTime and score to VBox 

        //Adding VBox to mainPane
        mainPane.setRight(rightVBox);
        BorderPane.setAlignment(rightVBox, Pos.CENTER);
        
        //Spawn the paper
        gameArea = new AnchorPane();
        //Setting height of paperPane
        gameArea.setMinWidth(((double)windowSize[0] * 0.8));
        gameArea.setMinHeight(((double)windowSize[1] * 0.8));

        //LOCHER
        String skin = spieler.getLocher().getSkin().getGuiBild();

        Image locher_skin = new Image("de/wolc/gui/images/" + skin);
        locher_new = new Rectangle();
        locher_new.setHeight(locher_skin.getHeight());
        locher_new.setWidth(locher_skin.getWidth());
        locher_new.setFill(new ImagePattern(locher_skin));

        AnchorPane.setBottomAnchor(locher_new, stage.getWidth() * 0.20);
        AnchorPane.setLeftAnchor(locher_new, stage.getHeight() * 0.65);

        // Benachrichtigungen
        benachrichtigungen = new Label();
        benachrichtigungen.setTextFill(Color.RED);
        benachrichtigungen.setFont(new Font(20));

        AnchorPane.setLeftAnchor(benachrichtigungen, stage.getWidth() * 0.40);
        AnchorPane.setBottomAnchor(benachrichtigungen, stage.getHeight() * 0.85);

        //Locher_new Mouse Events
        locher_new.setOnMouseClicked(e -> {
            //Abgleichen des gedrückten Buttons und des Cooldowns
            if (e.getButton() == MouseButton.PRIMARY) {
                double cooldown = spieler.getLocher().getCooldown();
                if (cooldown == 0) {
                    Lochprozess prozess = spieler.getLocher().lochen();
                    ArrayList<Konfetti> spielerKonfetti = spieler.getKonfetti();
                    spielerKonfetti.addAll(prozess.getKonfetti());

                    Bounds bounds = locher_new.getBoundsInParent();
                    for(Konfetti konfetti : prozess.getKonfetti()) {
                        konfettiObjekte.add(new KonfettiObjekt(this, konfetti, 
                            MultiUse.zufall(bounds.getMinX(), bounds.getMaxX()), 
                            MultiUse.zufall(bounds.getMaxY() - 5, bounds.getMaxY() + 5)));
                    }
                    
                    int locherPapierSize = locherPapier.size() - 1;
                    for (int i = 0; i <= locherPapierSize; i++) {
                        LocherPapierObjekt toCheckPapiere = locherPapier.get(i);
                        Papier toCheckPapier = toCheckPapiere.getPapier();
    
                        PapierStapel<?> currentStapel = spieler.getLocher().getStapel();
                        if (!currentStapel.istVorhanden(toCheckPapier)) {
                            toCheckPapiere.zerstoeren();
                            locherPapier.remove(i);
                            locherPapierSize--;
                            i--;
                        }
                    }

                    if(prozess.getWarZuGross()) {
                        this.benachrichtigungZeigen("Es sind zu viele Papiere eingelegt - [RECHTSKLICK] auf Locher zum entfernen!");
                        if(Gui.getEinstellungen().entitySoundEnabled()){
                            AudioClip clip = new AudioClip(MultiUse.url("de/wolc/gui/sounds/punch_error.wav"));
                            clip.play(100);
                        }
                    } else {
                        if(Gui.getEinstellungen().entitySoundEnabled()){
                            AudioClip clip = new AudioClip(MultiUse.url("de/wolc/gui/sounds/punch_1.wav"));
                            clip.play(100);
                        }
                    }
                } else {
                    this.benachrichtigungZeigen("Noch " + (Math.round(cooldown * 10d) / 10d) + "s auf Cooldown!");
                    if(Gui.getEinstellungen().entitySoundEnabled()){
                        AudioClip clip = new AudioClip(MultiUse.url("de/wolc/gui/sounds/punch_error.wav"));
                        clip.play(100);
                    }
                }
            }
            //Abgleichen des gedrückten Buttons
            if ( e.getButton() == MouseButton.SECONDARY) {
                PapierStapel<?> stapel = spieler.getLocher().getStapel();
                Papier removedPapier = stapel.entnehmen();

                //entfernen des Eingelgeten Bilds wenn kein Papier mehr im Locher
                if (removedPapier != null) {
                    new PapierObjekt(Game.this, removedPapier);
                    for (int i = 0; i <= locherPapier.size(); i++) {
                        LocherPapierObjekt todeltetPapier = locherPapier.get(i);
                        if (todeltetPapier.getPapier() == removedPapier){
                            todeltetPapier.zerstoeren();
                            locherPapier.remove(i);
                            break;
                        }
                    }
                }
            } else if(e.getButton() == MouseButton.MIDDLE && e.isShiftDown()) {
                // Shift & Middle mouse button --> Konfetti cheat
                for(int i = 0; i < RANDOM.nextInt(100); i++) {
                    this.spieler.getKonfetti().add(new Konfetti(Farbe.zufallsfarbe()));
                }
            }
        });


        //Add Nodes to the AnchorPane
        gameArea.getChildren().addAll(locher_new, benachrichtigungen);

        //Add the elements to the Main Pane
        mainPane.setCenter(gameArea);

        //Set Window Titel
        stage.setTitle(windowTitle);

        Platform.runLater(() -> this.start());

        return gameScene;
    }

    /**
     * Wird zu Ende des Spiels (im letzten Tick) aufgerufen.
     */
    public void spielEnde() {
        try {
            // Name ändern um den Bad Word filter vom Server anzuwenden.
            Leaderboard gesendet = Leaderboard.scoreSenden(this.spieler);
            this.spieler.setName(gesendet.getName());
            Gui.DB.speichern("spieler", this.spieler);
        } catch (Exception e) {
            Alert speichernFehler = new Alert(AlertType.WARNING);
            speichernFehler.setTitle("Fehler bei Spielstand speichern");
            speichernFehler.setHeaderText("Beim Speichern des Spielstandes ist ein Fehler aufgetreten. Hat " +
                "das Spiel Schreibrechte auf das eigene Verzeichnis?\nDer erzielte Fortschritt kann verloren gegangen" +
                "sein.");
            speichernFehler.setContentText(e.toString());
            speichernFehler.setResult(ButtonType.OK);
            speichernFehler.showAndWait();
            e.printStackTrace();
        }
        hintergrundMusik.stop();
        ItemShopMenu menu = new ItemShopMenu();
        this.stage.setScene(menu.ItemShopStage(this.stage));
        this.stage.setFullScreen(Gui.getEinstellungen().isVollbild());
    }

    /**
     * Wird zu Start des Spiels (im ersten Tick) aufgerufen.
     */
    private void spielStart() {
        // Für alle bereits existierende Papiere ein LocherPapierObjekt spawnen um diese anzuzeigen.
        for(int i = 0; i < this.spieler.getLocher().getStapel().groesse(); i++) {
            Papier papier = this.spieler.getLocher().getStapel().get(i);
            this.spawnLocherPapierObjekt(papier);
        }
    }

    public void spawnPapier() {
        int format = RANDOM.nextInt(3);
        Papier papier;
        if(format == 0) { papier = new A4(); }
        else if(format == 1) { papier = new A5(); }
        else { papier = new A6(); }
        papier.setFarbe(Farbe.zufallsfarbe());
        new PapierObjekt(Game.this, papier);
    }

    /**
     * Zeigt die gegebene Benachrichtung an. Dabei wird die vorherige Benachrichtigung überschrieben falls noch eine 
     * andere aktiv ist.
     * @param benachrichtigung Die Benachrichtigung welche anzeigt werden soll.
     */
    private void benachrichtigungZeigen(String benachrichtigung) {
        this.benachrichtigungenZeit = BENACHRICHTUNG_ANZEIGEZEIT;
        this.benachrichtigungen.setText(benachrichtigung);
    }

    /**
     * Prüft, ob sich die beiden übergebenen Shapes überschneiden, wenn ja, dann wird die Anzeige angepasst
     * @return true=collision vorhanden, false= keine collision vorhanden
     */
    private boolean checkForCollision(Shape shape1, Shape shape2){
        Shape ueberschneidung = Shape.intersect(shape1, shape2);
        return !ueberschneidung.getBoundsInLocal().isEmpty();
    }

    /**
     * Gibt das aktuell instalzierte AnchorPane zurück
     * @return Das aktuelle 'gameArea' AnchorPane
     */
    public AnchorPane getArea() {
        return this.gameArea;
    }

    public boolean checkForLocherCollision(Shape papier) {
        return checkForCollision(papier, locher_new);
    }

    public Spieler getCurrentSpieler(){
        return this.spieler;
    }

    @SuppressWarnings("unchecked")
    public void papierAufLocherGezogen(PapierObjekt objekt) {
        // penis 🍆 jh:😑
        Class<? extends Papier> papierTyp = objekt.getPapier().getClass();
        if (this.spieler.getLocher().getFormat() == papierTyp) {
            PapierStapel<?> stapel = this.spieler.getLocher().getStapel();
            boolean abgelegt;
            if(papierTyp == A4.class){
                PapierStapel<A4> stapel_A4 = (PapierStapel<A4>)stapel;
                abgelegt = stapel_A4.ablegen((A4)objekt.getPapier());
            }
            else if(papierTyp == A5.class){
                PapierStapel<A5> stapel_A5 = (PapierStapel<A5>)stapel;
                abgelegt = stapel_A5.ablegen((A5)objekt.getPapier());
            }
            else if(papierTyp == A6.class){
                PapierStapel<A6> stapel_A6 = (PapierStapel<A6>)stapel;
                abgelegt = stapel_A6.ablegen((A6)objekt.getPapier());
            } else {
                abgelegt = false;
            }
            if (abgelegt) {
                this.spawnLocherPapierObjekt(objekt.getPapier());
                objekt.zerstoeren();
            }
        }
    }

    /**
     * Spawnt ein Locherpapierobjekt für das gegebene Papier.
     * @param papier Das Papier für das das in den Locher eingelegte Locher Papierobjekt erstellt werden soll.
     */
    private void spawnLocherPapierObjekt(Papier papier) {
        this.locherPapier.add(new LocherPapierObjekt(this, this.spieler.getLocher().getStapel().groesse(), this.locher_new, papier));
    }

    /**
     * Wechselt das Papier anhand des ausgewählten neuen Formats
     * @param papierStapel - der neue Papierstapel des Formats
     * @param neuesPapierformat - das neue Format mit <Format>.class
     */
    private <T extends Papier> void papierWechsel(PapierStapel<T> papierStapel, Class<T> neuesPapierformat){
        // Alten Stapel rausnehmen
        PapierStapel<?> alterStapel = this.spieler.getLocher().entnehmen();
        if (alterStapel != null) {
            while(alterStapel.groesse() > 0) {
                alterStapel.entnehmen();
            }
        }
        // Neue Auswahl setzen
        this.spieler.getLocher().setFormat(neuesPapierformat);
        this.spieler.getLocher().einlegen(papierStapel);
    }

    /**
     * Entfernt alle aktuell in den Locher eingelegte Papiere.
     */
    private void locherPapierEntfernen() {
        for(int i = 0; i < this.locherPapier.size(); i++){
            LocherPapierObjekt papierObjekt = locherPapier.get(i);
            papierObjekt.zerstoeren();
        }
        this.locherPapier.clear();
    }

    /**
     * Wird jeden Tick aufgerufen.
     * @param jetztNanoZeit Die aktuelle Nano Zeit. Die vorherige für deltas findet sich in "letzteNanoZeit".
     */
    @Override
    public void handle(long jetztNanoZeit) {
        if (this.letzteNanoZeit == 0) {
            this.letzteNanoZeit = jetztNanoZeit;
            this.spielStart();
        }
        //Getting new and last TimeStamp in Miliseconds and calculating 
        long vergangenNanoZeit = jetztNanoZeit - this.letzteNanoZeit;
        double sekundenLetzterFrame = ((vergangenNanoZeit / 1000000000d));
        this.letzteNanoZeit = jetztNanoZeit;
        this.deltaZeit += sekundenLetzterFrame;

        // Durch das setzen einer FPS-Rate bekommen wir wesentlich bessere Performance.
        if (this.deltaZeit < 1d / ZIEL_FPS) {
            return;
        }
        this.fps = 1d / this.deltaZeit;

        //Triggering Cooldown and giving him the elapsedSeconds
        spieler.tick(this.deltaZeit);
        // Konfetti Objekte ticken
        int size = this.konfettiObjekte.size();
        for(int i = 0; i < size; i++) {
            KonfettiObjekt objekt = this.konfettiObjekte.get(i);
            objekt.tick(this.deltaZeit);
            if (objekt.istZerstoert()) {
                this.konfettiObjekte.remove(i);
                size--; 
                i--;
            }
        }

        this.timeToNextPapier -= this.deltaZeit;
        this.remainingTimeAvailable -= this.deltaZeit;
        this.benachrichtigungenZeit -= this.deltaZeit;

        if (this.benachrichtigungenZeit <= 0) {
            Game.this.benachrichtigungen.setText("");
            this.benachrichtigungenZeit = 0;
        }

        if (this.timeToNextPapier <= 0) {
            spawnPapier();
            this.timeToNextPapier = 0.5d + 2.5d * RANDOM.nextDouble();
        }

        //Check for end of Time
        if(this.remainingTimeAvailable <= 0) {
            this.stop();
            this.spielEnde();
        }

        this.updateLabels();

        this.deltaZeit = 0;
    }

}