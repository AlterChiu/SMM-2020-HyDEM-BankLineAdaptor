package Execution;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gdal.ogr.Geometry;

import geo.gdal.GdalGlobal;
import geo.gdal.SpatialReader;
import geo.gdal.SpatialWriter;
import geo.gdal.application.IrregularNetBasicControl;
import geo.gdal.application.IrregularNetBasicControl.EdgeClass;
import geo.gdal.application.IrregularNetBasicControl.FaceClass;
import geo.gdal.application.IrregularReachBasicControl;
import testFolder.SOBEK_OBJECT.SobekBankLine;

public class HyDEM_ReLinedBankLine {
	public static int dataDecimal = 4;

	public static void main(String[] args) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		// @ input
		// -HyDEM_SplitBankLinePolygon(String , spatailFile path)

		// @Output
		// -HyDEM_BankLine (String , spatailFile path)

//		Map<String, String> parameter = WorkSpace.settingVariables(args);
		Map<String, String> parameter = WorkSpace.settingVariables();

		String splitHydemPolygons = parameter.get("-HyDEM_SplitBankLinePolygon");
		String bankLineHydem = parameter.get("-HyDEM_BankLine");

		// <================================================>
		// <====== ReCraete pairs bankLine from splitHyDEM polygon ========>
		// <================================================>
		/*
		 * @ translate polygons to paired bankLine
		 * 
		 * @ input : splitHydemPolygons
		 * 
		 * @ output : bankLineHydem
		 */

		IrregularNetBasicControl splitBanKLinePolygons = new IrregularNetBasicControl(
				new SpatialReader(splitHydemPolygons));
		List<Geometry> outGeo = new ArrayList<>();

		// only store the key of end face, which used
		Set<String> usedFaceKey = new HashSet<>();

		// which directions not used while crossSection detection
		// key= leftEdgeKey , value = startFaceKey(crossSection)
		Map<String, String> leftEdgeFaceKeyMap = new HashMap<>();

		// only store the crossSection which used for main bankLine
		Set<String> passedCrossSectionFaceKey = new HashSet<>();

		Map<String, FaceClass> faceMap = splitBanKLinePolygons.getFaceMap();
		Map<String, EdgeClass> edgeMap = splitBanKLinePolygons.getEdgeMap();

		/*
		 * 
		 * there are three kind of detection
		 * 
		 * 1. start from end face, which face connects to one the other face only
		 * 
		 * 2. start from crossSection, which start from the "not main bankLine" of
		 * crossSection. And it will skip the detection which end up end face.
		 * 
		 * 3. start from crossSection, which start from the "main bankLine" of
		 * crossSection, this kind of detection only happen while looping. That means
		 * this bankLine start and end in the same crossSection
		 * 
		 */

		// first detection, start from end face
		// <--------------------------------------------------------->
		for (String faceKey : faceMap.keySet()) {

			// check is used or not
			if (!usedFaceKey.contains(faceKey)) {

				// start face
				FaceClass currentFace = faceMap.get(faceKey);
				if (currentFace.getLinkedFace().size() == 1) {

					// add end face key to used keySet
					usedFaceKey.add(currentFace.getFaceKey());

					// detect the bankLine
					BankLineDetector detector = new BankLineDetector(currentFace,
							currentFace.getLinkedEdge(currentFace.getLinkedFace().get(0)).get(0));

					// save main bankLine
					outGeo.add(detector.getMainBankLine());

					// add end face key to used keySet
					usedFaceKey.add(detector.getEndFaceKey());

					// add used crossSection to keySet
					detector.getPassedCrossSectionFaceID().forEach(faceID -> {
						passedCrossSectionFaceKey.add(faceID);
					});

					// add crossSection not used face key
					Map<String, String> temptLeftFaceKeyMap = detector.getLeftEdgeFaceMap();
					temptLeftFaceKeyMap.keySet().forEach(key -> {
						leftEdgeFaceKeyMap.put(key, temptLeftFaceKeyMap.get(key));
					});
				}
			}
		}

		// second detection, start from crossSection
		// <--------------------------------------------------------->
		while (leftEdgeFaceKeyMap.keySet().size() > 0) {

			String directionEdgeKey = new ArrayList<>(leftEdgeFaceKeyMap.keySet()).get(0);
			EdgeClass directionEdge = edgeMap.get(directionEdgeKey);
			FaceClass startFace = faceMap.get(leftEdgeFaceKeyMap.get(directionEdgeKey));

			BankLineDetector detector = new BankLineDetector(startFace, directionEdge);
			FaceClass endFace = faceMap.get(detector.getEndFaceKey());

			if (!endFace.isEnd()) {

				// save main bankLine
				outGeo.add(detector.getMainBankLine());

				// add end face key to used keySet
				usedFaceKey.add(detector.getEndFaceKey());

				// add used crossSection to keySet
				detector.getPassedCrossSectionFaceID().forEach(faceID -> {
					passedCrossSectionFaceKey.add(faceID);
				});

				// remove end edge from leftEdge
				try {
					leftEdgeFaceKeyMap.remove(detector.getLastDirection().getKey());
				} catch (Exception e) {
				}
			}

			// remove start edge from leftEdge
			try {
				leftEdgeFaceKeyMap.remove(directionEdgeKey);
			} catch (Exception e) {
			}
		}

		// third detection, start from crossSection, loop
		// <--------------------------------------------------------->
		// find the crossSection which not used for main bankLine
		Set<String> notUsedCrooSectionFaceID = new HashSet<>();
		faceMap.keySet().forEach(key -> {
			FaceClass temptFace = faceMap.get(key);
			// find crossSection
			if (temptFace.getLinkedFace().size() > 2) {

				// find not used for main bankLine
				if (!passedCrossSectionFaceKey.contains(temptFace.getFaceKey())) {
					notUsedCrooSectionFaceID.add(temptFace.getFaceKey());
				}
			}
		});

		// start detection
		while (notUsedCrooSectionFaceID.size() > 0) {
			FaceClass startFace = faceMap.get(new ArrayList<>(notUsedCrooSectionFaceID).get(0));

			// get the longest edge for the direction
			List<EdgeClass> availableDirection = getBankLineAvaliableDirection(startFace);
			Collections.sort(availableDirection, new Comparator<EdgeClass>() {
				@Override
				public int compare(EdgeClass e1, EdgeClass e2) {
					return (int) e2.getLength() - (int) e1.getLength();
				}
			});
			EdgeClass directionEdge = availableDirection.get(0);

			BankLineDetector detector = new BankLineDetector(startFace, directionEdge, true);

			// save main bankLine
			outGeo.add(detector.getMainBankLine());

			// remove passed crossSection, which used for main bankLine
			notUsedCrooSectionFaceID.remove(startFace.getFaceKey());
			detector.getPassedCrossSectionFaceID().forEach(faceID -> {
				notUsedCrooSectionFaceID.remove(faceID);
			});
		}

		// pairs bank line
		List<SobekBankLine> bankLines = new ArrayList<>();
		for (int index = 0; index < outGeo.size(); index++) {
			try {
				IrregularReachBasicControl temptReach = new IrregularReachBasicControl(outGeo.get(index));
				List<Geometry> singleLineString = temptReach.getReLinkedEdge();

				Geometry temptGeo1 = singleLineString.get(0);
				Geometry temptGeo2 = singleLineString.get(1);

				SobekBankLine temptBankLine1 = new SobekBankLine(temptGeo1);
				temptBankLine1.setID((index + 1) * 2);

				SobekBankLine temptBankLine2 = new SobekBankLine(temptGeo2);
				temptBankLine2.setID((index + 1) * 2 - 1);

				temptBankLine1.getDistance(temptBankLine2);

				bankLines.add(temptBankLine1);
				bankLines.add(temptBankLine2);
			} catch (Exception e) {
			}
		}

		// output shp
		SpatialWriter hyDEMBankLine = new SpatialWriter();
		hyDEMBankLine.addFieldType("ID", "Integer");
		hyDEMBankLine.addFieldType("LinkedID", "Integer");
		hyDEMBankLine.addFieldType("Direction", "Integer");

		for (int index = 0; index < bankLines.size(); index++) {
			Map<String, Object> attrTable = new HashMap<>();
			attrTable.put("ID", bankLines.get(index).getID());
			attrTable.put("LinkedID", bankLines.get(index).getLinkedPointID());
			attrTable.put("Direction", bankLines.get(index).getLinkedDirection());
			hyDEMBankLine.addFeature(bankLines.get(index).getGeo(), attrTable);
		}
		hyDEMBankLine.saveAsShp(bankLineHydem);

		System.out.println("create HyDEM bankLine, HyDEM_BankLine.shp");
	}

	public static class BankLineDetector {
		// key= leftEdgeKey , value = startFaceKey(crossSection)
		private Map<String, String> leftEdgeFaceKeyMap = new LinkedHashMap<>();
		private List<Geometry> mainBankLine = new ArrayList<>();
		private Set<String> passCrossSectionFaceID = new HashSet<>();

		private String endFaceKey = "";
		private EdgeClass lastDirection = null;
		private FaceClass currentFace;

		public BankLineDetector(FaceClass startFace, EdgeClass direction, boolean isThirdDection) {
			if (isThirdDection) {
				passCrossSectionFaceID.add(startFace.getFaceKey());
			}
			processing(startFace, direction);
		}

		public BankLineDetector(FaceClass startFace, EdgeClass direction) {
			processing(startFace, direction);
		}

		private void processing(FaceClass startFace, EdgeClass direction) {
			this.currentFace = startFace;

			// start detecting
			List<Geometry> temptGeoList = new ArrayList<>();

			FaceClass nextFace = direction.getOtherFace(currentFace);
			List<EdgeClass> nextFaceAvalibleDirection = getBankLineAvaliableDirection(nextFace);

			// which edge is contain by both face(current and next)
			EdgeClass directionEdge = currentFace.getLinkedEdge(nextFace).get(0);

			while (nextFaceAvalibleDirection.size() != 1) {
				/*
				 * 1 : end of the stream
				 * 
				 * 2 : basic type
				 * 
				 * >2 : crossSeciotn
				 */

				/*
				 * type basic
				 */
				if (nextFaceAvalibleDirection.size() == 2) {
					temptGeoList.add(createBankLineFromDirection(nextFace, nextFaceAvalibleDirection.get(0),
							nextFaceAvalibleDirection.get(1)));
				}

				/*
				 * type crossSection
				 */
				else if (nextFaceAvalibleDirection.size() > 2) {

					Collections.sort(nextFaceAvalibleDirection, new Comparator<EdgeClass>() {
						@Override
						public int compare(EdgeClass e1, EdgeClass e2) {
							int compare = (int) (e2.getLength() * Math.pow(10, dataDecimal))
									- (int) (e1.getLength() * Math.pow(10, dataDecimal));
							if (compare == 0) {
								return 1;
							} else {
								return compare;
							}
						}
					});

					List<EdgeClass> temptList = new ArrayList<>();
					temptList.add(nextFaceAvalibleDirection.get(0));
					temptList.add(nextFaceAvalibleDirection.get(1));

					// get not used face which on crossSection
					for (int index = 2; index < nextFaceAvalibleDirection.size(); index++) {
						this.leftEdgeFaceKeyMap.put(nextFaceAvalibleDirection.get(index).getKey(),
								nextFace.getFaceKey());
					}

					nextFaceAvalibleDirection.clear();
					nextFaceAvalibleDirection = temptList;

					// if available direction is include current direction,
					// add this crossSection face to current bankLine
					if (temptList.contains(directionEdge)) {

						// check for go back to same crossSection
						if (!passCrossSectionFaceID.contains(nextFace.getFaceKey())) {
							passCrossSectionFaceID.add(nextFace.getFaceKey());
							temptGeoList.add(createBankLineFromDirection(nextFace, nextFaceAvalibleDirection.get(0),
									nextFaceAvalibleDirection.get(1)));

							// if go back to same crossSection
							// make sure the last bank line does not connect to crossSection
						} else {
							Geometry temptGeo = temptGeoList.get(temptGeoList.size() - 1);
							temptGeoList.remove(temptGeoList.size() - 1);
							temptGeo = temptGeo.Difference(nextFace.getGeo().Buffer(1));
							temptGeoList.add(temptGeo);
							break;
						}

						// if not stop detecting bankLine
					} else {
						try {
							Geometry temptGeo = temptGeoList.get(temptGeoList.size() - 1);
							temptGeoList.remove(temptGeoList.size() - 1);
							temptGeo = temptGeo.Difference(nextFace.getGeo().Buffer(1));
							temptGeoList.add(temptGeo);
						} catch (Exception e) {
						}
						break;
					}

				}

				// go to next face
				currentFace = nextFace;
				nextFaceAvalibleDirection.remove(directionEdge);
				directionEdge = nextFaceAvalibleDirection.get(0);
				nextFace = directionEdge.getOtherFace(currentFace);
				nextFaceAvalibleDirection = getBankLineAvaliableDirection(nextFace);
			}

			// detect end
			this.endFaceKey = nextFace.getFaceKey();
			this.mainBankLine.add(GdalGlobal.GeometriesMerge(temptGeoList));
			this.lastDirection = directionEdge;
			try {
				this.leftEdgeFaceKeyMap.remove(directionEdge.getKey());
			} catch (Exception e) {
			}
		}

		public Map<String, String> getLeftEdgeFaceMap() {
			return this.leftEdgeFaceKeyMap;
		}

		public Geometry getMainBankLine() {
			return GdalGlobal.GeometriesMerge(this.mainBankLine);
		}

		public List<Geometry> getMainBankLineList() {
			return this.mainBankLine;
		}

		public EdgeClass getLastDirection() {
			return this.lastDirection;
		}

		public String getEndFaceKey() {
			return this.endFaceKey;
		}

		public List<String> getPassedCrossSectionFaceID() {
			return new ArrayList<>(this.passCrossSectionFaceID);
		}

	}

	private static List<EdgeClass> getBankLineAvaliableDirection(FaceClass faceClass) {
		List<EdgeClass> boundaries = new ArrayList<>();

		// get edge which linked to 2 polygons
		for (EdgeClass edge : faceClass.getLinkedEdge()) {
			if (edge.getLinkedFace().size() > 1) {
				boundaries.add(edge);
			}
		}
		return boundaries;
	}

	private static Geometry createBankLineFromDirection(FaceClass faceClass, EdgeClass edge1, EdgeClass edge2) {
		List<Geometry> outGeo = new ArrayList<>();

		// start from edge1
		IrregularNetBasicControl.NodeClass node1 = edge1.getLinkedNode().get(0);
		IrregularNetBasicControl.NodeClass node2 = edge1.getLinkedNode().get(1);

		// node1
		EdgeClass startEdge1 = edge1;
		while (!edge2.isContain(node1)) {
			for (EdgeClass otherEdge : node1.getOtherEdge(startEdge1)) {
				if (otherEdge.isLinked(faceClass)) {
					outGeo.add(otherEdge.getGeo());
					node1 = otherEdge.getOtherNode(node1);
					startEdge1 = otherEdge;
					break;
				}
			}
		}

		// node2
		EdgeClass startEdge2 = edge1;
		while (!edge2.isContain(node2)) {
			for (EdgeClass otherEdge : node2.getOtherEdge(startEdge2)) {
				if (otherEdge.isLinked(faceClass)) {
					outGeo.add(otherEdge.getGeo());
					node2 = otherEdge.getOtherNode(node2);
					startEdge2 = otherEdge;
					break;
				}
			}
		}

		// outGeo
		Geometry mergedPolygon = GdalGlobal.GeometriesMerge(outGeo);
		return mergedPolygon;
	}

}
