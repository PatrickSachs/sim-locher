import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.control.Button;
import javafx.geometry.Insets;
import javafx.geometry.*;

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;

public class StageMainMenu extends Application
{
    //Variables
    private final String windowTitle = "World of Locher Craft";
    
    @Override
    public void start(Stage stage) throws Exception{
        stage.setTitle(windowTitle);        

        //Generating and setting the Objects
        BorderPane mainPane = new BorderPane();

        //Background setzen
        BackgroundImage backgroundImageMainMenu = new BackgroundImage(new Image("images/locher_animated.gif"), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,BackgroundSize.DEFAULT);
        mainPane.setBackground(new Background(backgroundImageMainMenu));


        //Gruppe für die Buttons
        VBox buttonBox = new VBox();
        buttonBox.setPadding(new Insets(25,8,25,8));
        buttonBox.setSpacing(20);
        buttonBox.setPrefWidth(100);

        //Buttons
        Button button_playgame = new Button("Play_Game()");
            button_playgame.setMinWidth(buttonBox.getPrefWidth());
        Button button_settings = new Button("Settings()");
            button_settings.setMinWidth(buttonBox.getPrefWidth());
        Button button_exit = new Button("Exit()");
            button_exit.setMinWidth(buttonBox.getPrefWidth());


        //Adding the Buttons to the VBox and the VBox to the BorderPane
        buttonBox.getChildren().addAll(button_playgame, button_settings, button_exit);
        mainPane.setCenter(buttonBox);


        //Get Scene size and create a new Instance
        int[] screenSizes = GetScreenSize();
        Scene sceneMainWindow = new Scene(mainPane, (int)(screenSizes[0] * 0.5), (int)(screenSizes[1] * 0.5));
        

        //Put everything together
        stage.setScene(sceneMainWindow);
        
        //Show the Stage
        stage.show();
    }
    
    /**
     * Holt sich die größe aller angeschlossenen Monitore
     * @return int[width_screen1, height_screen1, width_screen2, height_screen2,...]
     */
    private int[] GetScreenSize(){
        //Define 
        GraphicsEnvironment graphEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] graphDev = graphEnv.getScreenDevices();
        
        //Variables
        int[] screenSizes = new int[(graphDev.length * 2)];
        int count = 0;
        
        for(int i = 0; i < graphDev.length; i++){
            screenSizes[count] = graphDev[i].getDisplayMode().getWidth();
            screenSizes[count + 1] = graphDev[i].getDisplayMode().getHeight();
            count = count + 2;
        }
        
        return screenSizes;
    }
    
}