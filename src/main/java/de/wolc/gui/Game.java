package de.wolc.gui;

import de.wolc.MultiUse;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.control.Button;
import javafx.geometry.Insets;
import javafx.geometry.*;
import javafx.event.*;
import javafx.scene.control.Labeled;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

public class Game{

    private final String windowTitle = "World of Locher Craft";
    private final String backgroundImageLocation = "images/Test_Bild.jpg";


    public Scene GameMainStage(Stage stage){
        //Main Orientation Node and initale settings
        BorderPane mainPane = new BorderPane();
        //mainPane.setStyle("-fx-background-repeat: repeat"); 

        //Setting and creating the new Scene
        Scene gameScene = new Scene(mainPane);

        //Set Fullscreen
        stage.setFullScreen(true);

        //Setting Background and width and height to screen
        MultiUse mu = new MultiUse();
        int[] windowSize = mu.GetScreenSize();
        Image backgroundImage = new Image(backgroundImageLocation, (double)windowSize[0], (double)windowSize[1], false, false);
        

        BackgroundImage backgroundImageGame = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,BackgroundSize.DEFAULT);
        mainPane.setBackground(new Background(backgroundImageGame));
        
        //Creating the Component-nodes
        //Creating the VBox for the right Output
        VBox rightVBox = new VBox();
        //Creating Label for outputing score
        Label score = new Label();
        score.setTextFill(Color.RED);
        score.setText("Score: 0"); 
        //Creating Label for remaining Time
        Label remainingTime = new Label();
        remainingTime.setTextFill(Color.RED);
        remainingTime.setText("Zeit: 00:00");
        //Adding remainingTime and score to VBox 
        rightVBox.getChildren().addAll(score, remainingTime);
        //Adding VBox to mainPane
        mainPane.setRight(rightVBox);
        mainPane.setAlignment(rightVBox, Pos.CENTER);

        return gameScene;
    }
}