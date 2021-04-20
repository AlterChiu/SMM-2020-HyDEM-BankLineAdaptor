package Execution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.gdal.ogr.Geometry;

import geo.gdal.application.IrregularReachBasicControl;
import geo.gdal.application.IrregularReachBasicControl.NodeClass;
import usualTool.AtCommonMath;

public class SOBEK_OBJECT {

	public static class SobekBankLine {
		private Geometry geo;
		private double[] firstPoint;
		private double[] secondPoint;

		private double distance = Double.POSITIVE_INFINITY;
		private int linkedDirection = -1; // 0 for HeadToHead , 1 for HeadToEnd
		private int linkedPointID = -1;
		private int id = -1;

		private Set<SobekBankPoint> bankPoints = new LinkedHashSet<>();
		private Map<String, Integer> nodeContainer = new LinkedHashMap<>();

		public SobekBankLine(Geometry geo) {
			this.geo = geo;
			this.firstPoint = new double[] { geo.GetX(0), geo.GetY(0) };
			this.secondPoint = new double[] { geo.GetX(geo.GetPointCount() - 1), geo.GetY(geo.GetPointCount() - 1) };
			for (int index = 0; index < geo.GetPointCount(); index++) {
				String xString = AtCommonMath.getDecimal_String(geo.GetX(index),
						IrregularReachBasicControl.dataDecimale);
				String yString = AtCommonMath.getDecimal_String(geo.GetY(index),
						IrregularReachBasicControl.dataDecimale);
				this.nodeContainer.put(xString + "_" + yString, index);
			}
		}

		public Geometry getGeo() {
			return this.geo;
		}

		public double[] getFirstPoint() {
			return this.firstPoint;
		}

		public double[] getSecondPoint() {
			return this.secondPoint;
		}

		public List<String> getPointKeys() {
			List<String> temptNodeList = new ArrayList<>();
			this.nodeContainer.keySet().forEach(key -> temptNodeList.add(key));
			return temptNodeList;
		}

		public void setID(int id) {
			this.id = id;
		}

		public int getID() {
			return this.id;
		}

		// 0 for HeadToHead , 1 for HeadToEnd
		public int getLinkedDirection() {
			return this.linkedDirection;
		}

		public void setLinkedDirection(int direction) {
			this.linkedDirection = direction;
		}

		public int getLinkedPointID() {
			return this.linkedPointID;
		}

		public void setDistance(double distance) {
			this.distance = distance;
		}

		public void setLinkedPointID(int linkedPoint) {
			this.linkedPointID = linkedPoint;
		}

		public double getDistance() {
			return this.distance;
		}

		public double getDistance(SobekBankLine otherBankLine) {
			// initialize distance
			this.distance = Double.POSITIVE_INFINITY;

			double[] firstPoint = otherBankLine.getFirstPoint();
			double[] seconPoint = otherBankLine.getSecondPoint();

			double lengthHeadToHead = Math.sqrt(
					Math.pow(firstPoint[0] - this.firstPoint[0], 2) + Math.pow(firstPoint[1] - this.firstPoint[1], 2));
			double lengthHeadToEnd = Math.sqrt(Math.pow(firstPoint[0] - this.secondPoint[0], 2)
					+ Math.pow(firstPoint[1] - this.secondPoint[1], 2));

			double distance = 0;
			if (lengthHeadToHead > lengthHeadToEnd) {
				distance = lengthHeadToEnd + Math.sqrt(Math.pow(seconPoint[0] - this.firstPoint[0], 2)
						+ Math.pow(seconPoint[1] - this.firstPoint[1], 2));
			} else {
				distance = lengthHeadToHead + Math.sqrt(Math.pow(seconPoint[0] - this.secondPoint[0], 2)
						+ Math.pow(seconPoint[1] - this.secondPoint[1], 2));
			}

			// check current distance
			if (this.distance > distance) {
				this.distance = distance;
				this.linkedPointID = otherBankLine.getID();

				// 0 for HeadToHead , 1 for HeadToEnd
				if (lengthHeadToHead > lengthHeadToEnd) {
					this.linkedDirection = 1;
				} else {
					this.linkedDirection = 0;
				}
			}

			// check other distance
			if (otherBankLine.getDistance() > distance) {
				otherBankLine.setDistance(distance);
				otherBankLine.setLinkedPointID(this.id);

				// 0 for HeadToHead , 1 for HeadToEnd
				if (lengthHeadToHead > lengthHeadToEnd) {
					otherBankLine.setLinkedDirection(1);
				} else {
					otherBankLine.setLinkedDirection(0);
				}
			}

			return this.distance;
		}

		// if not contain return -1
		public int isNodeContain(NodeClass node) {
			return Optional.ofNullable(this.nodeContainer.get(node.getId())).orElse(-1);
		}

		public int isNodeContain(String nodeName) {
			return Optional.ofNullable(this.nodeContainer.get(nodeName)).orElse(-1);
		}

		public void addBankPoint(Geometry geo) {
			SobekBankPoint temptBankPoint = new SobekBankPoint(geo);
			temptBankPoint.setBelongBankLineID(this.id);
			temptBankPoint.setIndexInBelongBankLine(this.nodeContainer.get(temptBankPoint.getID()));
			this.bankPoints.add(temptBankPoint);
		}

		public List<SobekBankPoint> getBankPoints() {
			return new ArrayList<>(this.bankPoints);
		}
	}

	public static class SobekBankPoint {
		private Geometry geo;
		private String id; // x_y
		private int belongBankLineID = -1;
		private int indexInBelongBankLine = -1;

		public SobekBankPoint(Geometry geo) {
			String xString = AtCommonMath.getDecimal_String(geo.GetX(), IrregularReachBasicControl.dataDecimale);
			String yString = AtCommonMath.getDecimal_String(geo.GetY(), IrregularReachBasicControl.dataDecimale);
			this.id = xString + "_" + yString;
			this.geo = geo;
		}

		public void setBelongBankLineID(int index) {
			this.belongBankLineID = index;
		}

		public void setIndexInBelongBankLine(int index) {
			this.indexInBelongBankLine = index;
		}

		public String getID() {
			return this.id;
		}

		public int getBelongBankLineID() {
			return this.belongBankLineID;
		}

		public int getIndexInBelongBankLine() {
			return this.indexInBelongBankLine;
		}

		public double getX() {
			return this.geo.GetX();
		}

		public double getY() {
			return this.geo.GetY();
		}

		public Geometry getGeo() {
			return this.geo;
		}

	}

}
