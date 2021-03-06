package edu.utah.sci.cyclist.core.util;

import org.controlsfx.glyphfont.FontAwesome;

import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import edu.utah.sci.cyclist.Cyclist;

public class GlyphRegistry {
	public final static String FONT_AWESOME_TTF_PATH = "assets/fontawesome-webfont.ttf";
    public final static String DEFAULT_ICON_SIZE = "14.0";
    public final static String DEFAULT_FONT_SIZE = "1em";
    public final static AwesomeIcon DEFAULT_UNKNOWN_ICON = AwesomeIcon.QUESTION_CIRCLE;

    static {
    	Font.loadFont(Cyclist.class.getResource(FONT_AWESOME_TTF_PATH).toExternalForm(), 10.0);
    }
    
	static public Label get(String name) {
		return get(name, DEFAULT_ICON_SIZE);
	}
	
	static public Label get(AwesomeIcon iconName) {
		return get(iconName,  DEFAULT_ICON_SIZE);
	}
	
	static public Label get(AwesomeIcon iconName, String size) {
		return get(iconName != null ? iconName.toString(): DEFAULT_UNKNOWN_ICON.toString(),  size);
	}
	
	static public Label get(String name, String size) {
		return get("FontAwesome", name, size);
	}
	
	static public Label get(String font, String name, String size) {
		Label label = new Label(name);
        label.getStyleClass().add("awesome-icon");
        label.setStyle("-fx-font-family:"+font+"; -fx-font-size: " + size + ";");
        
        return label;
	}
	
	static public Text getText(AwesomeIcon iconName) {
		Text text = new Text(iconName.toString());
		text.getStyleClass().add("awesome-icon");
		text.setStyle("-fx-font-family: FontAwesome; -fx-font-size: " + DEFAULT_ICON_SIZE + ";");
		return text;
	}
}
