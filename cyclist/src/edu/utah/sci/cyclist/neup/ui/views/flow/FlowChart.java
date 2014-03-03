package edu.utah.sci.cyclist.neup.ui.views.flow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import edu.utah.sci.cyclist.core.event.Pair;
import edu.utah.sci.cyclist.core.util.AwesomeIcon;
import edu.utah.sci.cyclist.core.util.GlyphRegistry;
import edu.utah.sci.cyclist.neup.model.Range;

public class FlowChart extends VBox {
	private static final String NET_CHART_LABEL = "Net Flow";
	private static final String COMMULATIVE_CHART_LABEL = "Commulative Flow";
	
	private HBox _header;
	private StackPane _stack;
	private Pane _glass;
//	private Label _popup;
	private Rectangle _rec;
	
	private LineChart<Number, Number> _chart;
	private NumberAxis _xAxis;
	private NumberAxis _yAxis;
	private double _scale = 1;
	private boolean _opened = true;
	private String _type;
	private int _upperBound;
	private boolean _updating = false;
	
	private Map<FlowEntry, ChartInfo> _info = new HashMap<>();
	private ObjectProperty<Range<Integer>> _timeRangeProperty = new SimpleObjectProperty<>();
	
	public class ChartInfo {
		public Label title;
		public Collection<Pair<Integer, Double>> values;
		public XYChart.Series<Number, Number> series;
		public double scale;
		public int last;
	}
	
	public FlowChart() {
		super();
		build();
	}
	
	public ObjectProperty<Range<Integer>> timeRangeProperty() {
		return _timeRangeProperty;
	}
	
	private void selectChartType(String value) {
		_type = value;
		double s = 1;
		for (ChartInfo info : _info.values()) {
			s = Math.max(s, computeScale(info.values));
		}
		if (s != _scale) {
			_scale = s;
			updateYAxis();
		}
		updateAll();
	}
	
	public void add(FlowEntry entry, String title, Collection<Pair<Integer, Double>> values) {
		if (values.size() == 0) return;
		
		int last = 0;
		for (Pair<Integer, Double> p : values) {
			last = Math.max(last, p.v1);
		}
		if (last > _upperBound) {
			_upperBound = last;
		}
		XYChart.Series<Number, Number> series = new XYChart.Series<>();
		Color c = entry.getColor();
		String style = c.toString().replace("0x", "#");
		series.nodeProperty().addListener(o->{
			series.getNode().setStyle("-fx-stroke:"+style);
		});

		double scale = computeScale(values); // relative to the current chart type
		
		if (scale > _scale) {
			updateScale(scale);
		}
		updateSeries(series, values);

		ChartInfo info = new ChartInfo();
		info.values = values;
		info.title = new Label(title);
		info.title.setStyle("-fx-background-color:"+style);
		info.title.getStyleClass().add("header");
		info.series = series;
		info.scale = scale;
		info.last = last;
		_info.put(entry, info);
		
		_header.getChildren().add(info.title);
		_chart.getData().add(series);
		_rec.setVisible(true);
	}
	
	public void remove(FlowEntry entry) {
		ChartInfo info = _info.remove(entry);
		if (info == null) {
			return;
		}
				
		_header.getChildren().remove(info.title);
		_chart.getData().remove(info.series);
		double s = 1;
		_upperBound = 0;
		for (ChartInfo ci : _info.values()) {
			s = Math.max(ci.scale, s);
			_upperBound = Math.max(_upperBound, ci.last);
		}
		if (s != _scale) {
			updateScale(s);
			updateYAxis();
		}
		if (_info.isEmpty()) {
			_rec.setVisible(false);
		}
	}
	
	private void updateAll() {
		for (ChartInfo info : _info.values()) {
			updateSeries(info.series, info.values);
		}
	}
	
	private void updateScale(double value) {
		_scale = value;
		System.out.println("scale: "+_scale);
		updateYAxis();
		updateAll();
	}

	private void updateYAxis() {
		String label = _scale == 1 ? "kg"
				: _scale == 1000 ? "x 1000 kg"
				: String.format("x %.0e kg", _scale);
	
		_yAxis.setLabel(label);
	}

	
	private double computeScale(Collection<Pair<Integer, Double>> values) {
		double max = 0;
		if (_type.equals(COMMULATIVE_CHART_LABEL)) {
			double sum = 0;
			for (Pair<Integer, Double> value : values) {
				sum += Math.abs(value.v2);
			}
			max = sum;
		} else {
			double prev = 0;
			for (Pair<Integer, Double> value : values) {
				max = Math.max(max, Math.abs(value.v2-prev));
				prev = value.v2;
			}
		}
		if (max == 0) max = 1;
		double s = Math.pow(10, 3*Math.floor(Math.log10(max)/3));
		return s;
	}
	
	private void updateSeries(XYChart.Series<Number, Number> series, Collection<Pair<Integer, Double>> values) {
		series.getData().clear();

		if (_type.equals(COMMULATIVE_CHART_LABEL)) {
			double sum = 0;
			for (Pair<Integer, Double> value : values) {
				sum += value.v2/_scale;
				series.getData().add(new XYChart.Data<Number, Number>(value.v1, sum));
			}
		} else {
			double prev = 0;
			for (Pair<Integer, Double> value : values) {
				double v = value.v2/_scale;
				series.getData().add(new XYChart.Data<Number, Number>(value.v1, v-prev));
				prev = v;
			}
		}
	}

			
	private void build() {
		getStyleClass().add("fchart");
		
		buildHeader();
		
		getChildren().addAll(
			_header,
			buildChart()
		);
		
		addListeners();
	}
	
	private void buildHeader() {
		_header = new HBox();
		_header.getStyleClass().add("infobar");

		Label caret = new Label("", GlyphRegistry.get(AwesomeIcon.CARET_DOWN));
		
		ChoiceBox<String> type = new ChoiceBox<>();
		type.getStyleClass().add("choice");
		type.getItems().addAll(COMMULATIVE_CHART_LABEL, NET_CHART_LABEL);
	
		_header.getChildren().addAll(caret, type);
		
		caret.setOnMouseClicked(e->{
			_opened = !_opened;
			if (_opened) {
				caret.setGraphic(GlyphRegistry.get(AwesomeIcon.CARET_DOWN));
			} else {
				caret.setGraphic(GlyphRegistry.get(AwesomeIcon.CARET_RIGHT));			
			}
			_stack.setVisible(_opened);
			_stack.setManaged(_opened);
		});
		
		type.valueProperty().addListener(e->{
			selectChartType(type.getValue());
		});
		
		type.setValue(COMMULATIVE_CHART_LABEL);
	}
	

	private Node buildChart() {
		_xAxis = new NumberAxis();
		_xAxis.setLabel("time");
		_xAxis.setAnimated(false);
		
		_yAxis = new NumberAxis();
		_yAxis.setLabel("Cummulative");
		_yAxis.setAnimated(false);
		
		_chart = new LineChart<>(_xAxis, _yAxis);
		_chart.getStyleClass().add("chart");
		_chart.setCreateSymbols(false);
		
		_rec = new Rectangle();
		_rec.getStyleClass().add("range");
		_rec.yProperty().bind(_yAxis.layoutYProperty().add(5));
		_rec.heightProperty().bind(_yAxis.heightProperty());
		_rec.setVisible(false);
		
//		_popup = new Label();
//		_popup.getStyleClass().add("popup");
//		_popup.setVisible(false);
//		_popup.layoutXProperty().bind(_fromLine.startXProperty().add(10));
		_glass = new Pane();
		_glass.getChildren().addAll(/*_fromLine*/_rec/*, _popup*/);
		
		_xAxis.layoutBoundsProperty().addListener(e->{
			updateRangeLater();		
		});
		
		_xAxis.lowerBoundProperty().addListener(o->{
			updateRangeLater();				
		});
		
		_xAxis.upperBoundProperty().addListener(o->{
			updateRangeLater();		
		});
		
		_stack = new StackPane();
		_stack.getChildren().addAll( _chart, _glass);
		_stack.setStyle("-fx-pref-height: 100px");
		
//		VBox.setVgrow(_stack, Priority.ALWAYS);
		return _stack;
	}
	
	private void updateRangeLater() {
		Platform.runLater(new Runnable() {
            @Override
            public void run() {
            	updateRange();
            }
		});
	}
	
	private void updateRange() {
		Range<Integer> r = timeRangeProperty().get();
		double t1 = r.from;
		double t2 = r.to+1;
		
		double x1 = _xAxis.getDisplayPosition(t1);
		double x2 = _xAxis.getDisplayPosition(t2);
		
		Point2D p1 = _glass.sceneToLocal(_xAxis.localToScene(x1, 0));
		Point2D p2 = _glass.sceneToLocal(_xAxis.localToScene(x2, 0));
		
		_rec.setX(p1.getX());
		_rec.setWidth(p2.getX() - p1.getX());
//		System.out.println("rec: "+_rec.getX()+", "+_rec.getY()+"  "+_rec.getWidth()+"x"+_rec.getHeight());
	}
	
	double _mx;
	double _offset;
	
	private void addListeners() {
		_timeRangeProperty.addListener(o->{
			if (_updating) return;
			updateRange();
		});
		
		_rec.setOnMousePressed(e->{
			_mx = e.getX();
			_offset = _xAxis.getDisplayPosition(_timeRangeProperty.get().from);
		});
		
		_rec.setOnMouseDragged(e->{
			double dx = e.getX() - _mx;
			Point2D p = _xAxis.sceneToLocal(_glass.localToScene(_rec.getX()+dx, 0));
			Number v = _xAxis.getValueForDisplay(p.getX());
			int i = v.intValue();
			_rec.setX(_rec.getX()+dx);
			_mx = e.getX(); 
		});
		
		_rec.setOnMouseReleased(e->{
			Point2D p = _xAxis.sceneToLocal(_glass.localToScene(_rec.getX(), 0));
			Number v = _xAxis.getValueForDisplay(p.getX());
			int i = v.intValue();
//			i = Math.max(0, Math.min(_upperBound, i));
			// nudge rectangle to an integer boundry
			double xi = _xAxis.getDisplayPosition(i);
			Point2D p1 = _xAxis.localToScene(xi,0);
			Point2D p2 = _glass.sceneToLocal(p1);
			_rec.setX(p2.getX());
			
			Range<Integer> r = _timeRangeProperty.get();
			Range<Integer> nr = new Range<>(i, i+r.to-r.from);
			_updating = true;
			_timeRangeProperty.set(nr);
			_updating = false;
		});
		
//		_fromLine.setOnMousePressed(e->{
//			_popup.setText(Integer.toString(_fromTimeProperty.get()));
//			_popup.setLayoutY(e.getY()-10);
//			_popup.setVisible(true);
//			_movingTime = _fromTimeProperty.get();
//		});
//		
//		_fromLine.setOnMouseDragged(e->{
//			
//			_popup.setLayoutY(e.getY()-10);
//			Point2D p = new Point2D(e.getSceneX(), e.getSceneY());
//			Number v = _xAxis.getValueForDisplay(_xAxis.sceneToLocal(p).getX());
//			int i = v.intValue();
//			i = Math.max(0, Math.min(_upperBound, i));
//			if (i>0 && i <= _upperBound) {
//				_fromLine.setStartX(e.getX());
//				_popup.setText(Integer.toString(i));
//				_movingTime = i;
//			}
//		});
//		
//		_fromLine.setOnMouseReleased(e->{
//			_popup.setVisible(false);
//			_fromTimeProperty.set(_movingTime);
//		});
	}
}