package org.cimug.compare.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.cimug.compare.AssociationProperties;
import org.cimug.compare.AttributeProperties;
import org.cimug.compare.ClassProperties;
import org.cimug.compare.DiagramProperties;
import org.cimug.compare.DiffUtils;
import org.cimug.compare.GeneralizationProperties;
import org.cimug.compare.NamedTypeComparator;
import org.cimug.compare.PackageProperties;
import org.cimug.compare.Status;
import org.cimug.compare.app.CIMModelComparisonGenerator.DiagramImage;
import org.cimug.compare.logs.CompareItem;
import org.cimug.compare.logs.ComparePackage;
import org.cimug.compare.logs.CompareResults;
import org.cimug.compare.logs.EACompareLog;
import org.cimug.compare.logs.Properties;
import org.cimug.compare.logs.Property;
import org.cimug.compare.uml1_3.AssociationEndType;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.AttributeType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.Diagram;
import org.cimug.compare.uml1_3.DiagramElement;
import org.cimug.compare.uml1_3.DiagramElementType;
import org.cimug.compare.uml1_3.GeneralizationType;
import org.cimug.compare.uml1_3.PackageType;
import org.cimug.compare.uml1_3.ifaces.GUIDIdentifier;
import org.cimug.compare.xmi1_1.XMIContentType;

class GUIDBasedDiffReportGeneratorImpl implements DiffReportGenerator {

	private XMIContentType baselineContentType;
	private XMIContentType targetContentType;

	private PreProcessor preProcessor;
	private File baselineImagesDir;
	private File destinationImagesDir;
	private DiagramImage imageType;
	private File outputFile;

	public GUIDBasedDiffReportGeneratorImpl(XMIContentType baselineContentType, XMIContentType targetContentType,
			File outputFile, DiagramImage imageType) throws JAXBException {
		this.baselineContentType = baselineContentType;
		this.targetContentType = targetContentType;
		this.preProcessor = new PreProcessor(baselineContentType, targetContentType);
		this.baselineImagesDir = new File(outputFile.getParent(), "Images-baseline");
		this.destinationImagesDir = new File(outputFile.getParent(), "Images-destination");
		this.imageType = imageType;
		this.outputFile = outputFile;
	}

	/**
	 * Special static class used purely for sorting purposes. The hashCode and
	 * equals methods are specialized for the purposes of storing Attribute specific
	 * CompareItem(s) in Sets or Maps.
	 */
	static class AttributeCompareItem {

		private CompareItem compareItem;

		public AttributeCompareItem(CompareItem compareItem) {
			this.compareItem = compareItem;
		}

		public CompareItem getCompareItem() {
			return this.compareItem;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((compareItem.getGuid() == null) ? 0 : compareItem.getGuid().hashCode());
			result = prime * result + ((compareItem.getName() == null) ? 0 : compareItem.getName().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AttributeCompareItem other = (AttributeCompareItem) obj;
			if (compareItem.getGuid() == null) {
				if (other.getCompareItem().getGuid() != null)
					return false;
			} else if (!compareItem.getGuid().equals(other.getCompareItem().getGuid()))
				return false;
			if (compareItem.getName() == null) {
				if (other.getCompareItem().getName() != null)
					return false;
			} else if (!compareItem.getName().equals(other.getCompareItem().getName()))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "AttributeCompareItem [compareItem=" + (compareItem != null ? compareItem.getName() : "") + "]";
		}
		
		

	}

	@Override
	public void processDiffReport() {
		try {
			EACompareLog compareLog = processDiffReport(baselineContentType, targetContentType);
			writeCompareLogFile(compareLog);
		} catch (FileNotFoundException | JAXBException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void writeCompareLogFile(EACompareLog compareLog) throws JAXBException, FileNotFoundException {
		JAXBContext jaxbContext = JAXBContext.newInstance(EACompareLog.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		if (this.outputFile != null) {
			jaxbMarshaller.marshal(compareLog, new PrintWriter(this.outputFile));
		} else {
			jaxbMarshaller.marshal(compareLog, new PrintWriter(System.out));
		}
	}

	private EACompareLog processDiffReport(XMIContentType theBaselineContentType, XMIContentType theTargetContentType) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd H:mm:ss");
		String dateTime = formatter.format(new Date());

		CompareResults compareResults = parseModelRootPackage(theBaselineContentType, theTargetContentType);

		ComparePackage comparePackage = new ComparePackage(compareResults, theTargetContentType.getModel().getName(),
				DiffUtils.convertRootModelXmiIdToEAGUID(targetContentType.getModel().getXmiId()), "1", "system",
				dateTime);

		EACompareLog compareLog = new EACompareLog(comparePackage);
		return compareLog;
	}

	private CompareResults parseModelRootPackage(XMIContentType theBaselineContentType,
			XMIContentType theTargetContentType) {

		boolean hasChanges = true;

		Status rootPackageStatus = (!theBaselineContentType.getModel().getXmiId()
				.equals(theTargetContentType.getModel().getXmiId()) ? Status.Changed : Status.Identical);

		CompareItem theRootPackage = new CompareItem(null, theBaselineContentType.getModel().getName(), "Package",
				theTargetContentType.getModel().getGUID(), rootPackageStatus.toString());

		/**
		 * =========================================================================
		 * Process child packages.
		 * =========================================================================
		 */
		List<PackageType> baselineChildPackages = theBaselineContentType.getModel().getPackages();
		List<PackageType> targetChildPackages = theTargetContentType.getModel().getPackages();

		for (PackageType targetChildPackage : targetChildPackages) {
			theRootPackage.getCompareItem().add(parsePackage(targetChildPackage));
		}

		/**
		 * Process any packages deleted from the baseline.
		 */
		for (PackageType baselineChildPackage : baselineChildPackages) {
			if (preProcessor.getBaselineDeletedPackagesXmiIds().containsKey(baselineChildPackage.getXmiId())) {
				theRootPackage.getCompareItem().add(parsePackage(baselineChildPackage));
			}
		}

		/**
		 * =========================================================================
		 * Process child classes.
		 * =========================================================================
		 */
		List<ClassType> baselineChildClasses = theBaselineContentType.getModel().getClasses();
		List<ClassType> targetChildClasses = theTargetContentType.getModel().getClasses();

		for (ClassType targetChildClass : targetChildClasses) {
			PackageType targetParentPackage = preProcessor.getTargetPackagesXmiIds()
					.get(targetChildClass.getParentPackageGUID());
			theRootPackage.getCompareItem().add(parseClass(targetChildClass, targetParentPackage));
		}

		/**
		 * Process any classes deleted from the baseline.
		 */
		for (ClassType baselineChildClass : baselineChildClasses) {
			if (preProcessor.getBaselineDeletedClassesXmiIds().containsKey(baselineChildClass.getXmiId())) {
				PackageType baselineParentPackage = preProcessor.getBaselinePackagesXmiIds()
						.get(baselineChildClass.getParentPackageGUID());
				theRootPackage.getCompareItem().add(parseClass(baselineChildClass, baselineParentPackage));
			}
		}

		CompareResults compareResults = new CompareResults(theRootPackage, hasChanges);
		return compareResults;
	}

	private CompareItem parsePackage(PackageType aPackage) {
		CompareItem thePackage = null;

		// Very first thing we do is to first determine if the package passed in is a
		// deleted package...
		if (preProcessor.getBaselineDeletedPackagesXmiIds().containsKey(aPackage.getXmiId())) {
			PackageProperties deletedPackageProperties = new PackageProperties(aPackage, null);
			Properties properties = deletedPackageProperties.getProperties();

			thePackage = new CompareItem(properties, aPackage.getName(), "", aPackage.getGUID(),
					Status.BaselineOnly.toString());

			for (PackageType childPackage : aPackage.getPackages()) {
				// Special process for a deleted parent package -- i.e. we only included those
				// child packages and child classes that also were deleted (and not those that
				// have been moved)...
				if (preProcessor.getBaselineDeletedPackagesXmiIds().containsKey(childPackage.getXmiId())) {
					thePackage.getCompareItem().add(parsePackage(childPackage));
				}
			}

			for (ClassType childClass : aPackage.getClasses()) {
				// Special process for a deleted child classes -- i.e. we only included those
				// child classes that also were deleted (and not those that have been moved)...
				if (preProcessor.getBaselineDeletedClassesXmiIds().containsKey(childClass.getXmiId())) {
					thePackage.getCompareItem().add(parseClass(childClass, aPackage));
				}
			}

			for (Diagram deletedDiagram : preProcessor.getBaselineDeletedDiagramsXmiIds().values()) {
				// Special process for a deleted child diagrams -- i.e. we only included those
				// child diagrams that also were deleted (and not those that have been moved)...
				if (aPackage.getXmiId().equals(deletedDiagram.getOwner())) {
					thePackage.getCompareItem().add(parseDiagram(deletedDiagram, aPackage));
				}
			}
		} else {
			// Retrieve the baselinePackage in order to process package properties...
			PackageType baselinePackage = (preProcessor.getBaselinePackagesXmiIds().containsKey(aPackage.getXmiId())
					? preProcessor.getBaselinePackagesXmiIds().get(aPackage.getXmiId())
					: null);

			PackageProperties packageProperties = new PackageProperties(baselinePackage, aPackage);
			Properties properties = packageProperties.getProperties();

			thePackage = new CompareItem(properties, aPackage.getName(), "Package", aPackage.getGUID(),
					packageProperties.getStatus().toString());

			List<PackageType> targetChildPackages = aPackage.getPackages();

			for (PackageType targetChildPackage : targetChildPackages) {
				thePackage.getCompareItem().add(parsePackage(targetChildPackage));
			}

			// Next we search for any child packages of this target package that were
			// deleted. This is determined by scanning all deleted baseline packages who's
			// parent package GUID is that of the target package...
			for (PackageType deletedChildPackage : preProcessor.getBaselineDeletedPackagesXmiIds().values()) {
				// Special process for a deleted parent package -- i.e. we only included those
				// child packages and child classes that also were deleted (and not those that
				// have been moved)...
				if (aPackage.getXmiId().equals(deletedChildPackage.getParentPackageGUID())) {
					thePackage.getCompareItem().add(parsePackage(deletedChildPackage));
				}
			}

			for (ClassType targetChildClass : aPackage.getClasses()) {
				thePackage.getCompareItem().add(parseClass(targetChildClass, aPackage));
			}

			for (ClassType baselineDeletedClass : preProcessor.getBaselineDeletedClassesXmiIds().values()) {
				// Special process for a deleted child classes -- i.e. we only included those
				// child classes that also were deleted (and not those that have been moved)...
				if (aPackage.getXmiId().equals(baselineDeletedClass.getParentPackageGUID())) {
					PackageType baselineParentPackage = preProcessor.getBaselinePackagesXmiIds()
							.get(aPackage.getXmiId());
					thePackage.getCompareItem().add(parseClass(baselineDeletedClass, baselineParentPackage));
				}
			}

			for (Diagram targetChildDiagram : preProcessor.getBaselineDiagramsXmiIds().values()) {
				if (aPackage.getXmiId().equals(targetChildDiagram.getOwner()) && //
						!preProcessor.getBaselineDeletedDiagramsXmiIds().containsKey(targetChildDiagram.getXmiId()) && //
						!preProcessor.getTargetMovedDiagramsXmiIds().containsKey(targetChildDiagram.getXmiId())) {
					thePackage.getCompareItem().add(parseDiagram(targetChildDiagram, aPackage));
				}
			}
			
			/** We need a second for loop to ensure we include any diagrams that may have been moved to this package **/
			for (Diagram targetChildDiagram : preProcessor.getTargetMovedDiagramsXmiIds().values()) {
				if (aPackage.getXmiId().equals(targetChildDiagram.getOwner()) && !preProcessor
						.getBaselineDeletedDiagramsXmiIds().containsKey(targetChildDiagram.getXmiId())) {
					thePackage.getCompareItem().add(parseDiagram(targetChildDiagram, aPackage));
				}
			}

			for (Diagram baselineDeletedDiagram : preProcessor.getBaselineDeletedDiagramsXmiIds().values()) {
				// Special process for a deleted child diagrams -- i.e. we only included those
				// child diagrams that also were deleted (and not those that have been moved)...
				//
				// NOTE: The getOwner() method returns an xmiId thus the different conditional.
				if (aPackage.getXmiId().equals(baselineDeletedDiagram.getOwner())) {
					PackageType baselineParentPackage = preProcessor.getBaselinePackagesXmiIds()
							.get(aPackage.getXmiId());
					thePackage.getCompareItem().add(parseDiagram(baselineDeletedDiagram, baselineParentPackage));
				}
			}

			for (Diagram targetNewDiagram : preProcessor.getTargetNewDiagramsXmiIds().values()) {
				// Special process for new child diagrams.
				//
				// NOTE: The getOwner() method returns an xmiId thus the different conditional.
				if (aPackage.getXmiId().equals(targetNewDiagram.getOwner())) {
					PackageType targetParentPackage = preProcessor.getTargetPackagesXmiIds().get(aPackage.getXmiId());
					thePackage.getCompareItem().add(parseDiagram(targetNewDiagram, targetParentPackage));
				}
			}
		}

		return thePackage;
	}

	private CompareItem parseClass(ClassType aClass, PackageType classParentPackage) {
		CompareItem theClass = null;

		// Very first thing we do is to first determine if the package passed in is a
		// deleted package...
		if (preProcessor.getBaselineDeletedClassesXmiIds().containsKey(aClass.getXmiId())) {
			ClassProperties deletedClassProperties = new ClassProperties(aClass, classParentPackage, null, null);
			Properties properties = deletedClassProperties.getProperties();

			theClass = new CompareItem(properties, aClass.getName(), "", aClass.getGUID(),
					Status.BaselineOnly.toString());

			/** Process the Class's attributes... */
			Set<String> sortedAttributeNames = new TreeSet<String>(new org.cimug.compare.StringComparator());

			List<AttributeType> classAttributes = aClass.getAttributes();
			classAttributes.forEach(attribute -> sortedAttributeNames.add(attribute.getName()));

			for (AttributeType baselineAttribute : classAttributes) {
				theClass.getCompareItem().add(parseAttribute(baselineAttribute, null));
			}

			// Process the Class's links (i.e. associations, generalizations, aggregations,
			// dependencies)...
			theClass.getCompareItem().add(parseLinks(aClass, classParentPackage, null, null));
		} else {
			// Retrieve the baselineClass in order to process class properties...
			ClassType baselineClass = (preProcessor.getBaselineClassesXmiIds().containsKey(aClass.getXmiId())
					? preProcessor.getBaselineClassesXmiIds().get(aClass.getXmiId())
					: null);

			PackageType baselineParentPackage = (baselineClass != null
					? preProcessor.getBaselinePackagesXmiIds().get(baselineClass.getXmiId())
					: null);

			ClassProperties classProperties = new ClassProperties(baselineClass, baselineParentPackage, aClass,
					classParentPackage);
			Properties properties = classProperties.getProperties();

			theClass = new CompareItem(properties, aClass.getName(), "Class", aClass.getGUID(),
					classProperties.getStatus().toString());

			Set<AttributeType> sortedAttributeTypes = new TreeSet<AttributeType>(
					new NamedTypeComparator<AttributeType>());

			switch (classProperties.getStatus())
				{
				case BaselineOnly:
					baselineClass.getAttributes().forEach(attribute -> sortedAttributeTypes.add(attribute));

					for (AttributeType baselineAttribute : sortedAttributeTypes) {
						theClass.getCompareItem().add(parseAttribute(baselineAttribute, null));
					}
					break;
				case ModelOnly:
					aClass.getAttributes().forEach(attribute -> sortedAttributeTypes.add(attribute));

					for (AttributeType targetAttribute : sortedAttributeTypes) {
						theClass.getCompareItem().add(parseAttribute(null, targetAttribute));
					}
					break;
				default:
					Set<AttributeCompareItem> sortedCompareItems = new TreeSet<AttributeCompareItem>(
							new Comparator<AttributeCompareItem>() {
								public int compare(AttributeCompareItem item1, AttributeCompareItem item2) {
									if (item1 == item2) {
										return 0;
									}
									if (item1 == null) {
										return -1;
									}
									if (item2 == null) {
										return 1;
									}

									String item1Type = item1.getCompareItem().getType();
									String item2Type = item2.getCompareItem().getType();

									String item1Name = item1.getCompareItem().getName();
									String item2Name = item2.getCompareItem().getName();

									if (item1Name == null && item2Name == null) {
										return 0;
									}
									if (item1Name == null) {
										return -1;
									}
									if (item2Name == null) {
										return 1;
									}

									if (item1Name.equals(item2Name)) {
										if (item1Type == null && item2Type == null) {
											return 0;
										}
										if (item1Type == null) {
											return -1;
										}
										if (item2Type == null) {
											return 1;
										}
										return item1Type.compareTo(item2Type);
									}

									return item1Name.compareTo(item2Name);
								}
							});

					/** We loop through all target attributes... */
					for (AttributeType targetAttribute : aClass.getAttributes()) {
						AttributeType baselineAttribute = baselineClass.getAttributeByGUID(targetAttribute.getGUID());
						sortedCompareItems
								.add(new AttributeCompareItem(parseAttribute(baselineAttribute, targetAttribute)));
					}

					/** We now identify deleted attributes... */
					for (AttributeType baselineAttribute : baselineClass.getAttributes()) {
						// If null then the attribute was deleted and doesn't appear in the target
						// class...
						if (aClass.getAttributeByGUID(baselineAttribute.getGUID()) == null) {
							sortedCompareItems.add(new AttributeCompareItem(parseAttribute(baselineAttribute, null)));
						}
					}

					/**
					 * Finally, we add the final sorted compare items to the list in sorted order
					 */
					for (AttributeCompareItem item : sortedCompareItems) {
						theClass.getCompareItem().add(item.getCompareItem());
					}
					break;
				}

			// Process the Class's links (i.e. associations, generalizations,
			// aggregations)...
			theClass.getCompareItem().add(parseLinks(baselineClass, baselineParentPackage, aClass, classParentPackage));
		}

		return theClass;
	}

	private CompareItem parseDiagram(Diagram aDiagram, PackageType diagramParentPackage) {
		CompareItem theDiagram = null;
		
		Status diagramStatus;
		
		// Very first thing we do is to first determine if the diagram passed in is a
		// deleted diagram...
		if (preProcessor.getBaselineDeletedDiagramsXmiIds().containsKey(aDiagram.getXmiId())) {
			DiagramProperties deletedDiagramProperties = new DiagramProperties(aDiagram, diagramParentPackage, null,
					null);
			Properties properties = deletedDiagramProperties.getProperties();

			diagramStatus = Status.BaselineOnly;
			
			theDiagram = new CompareItem(properties, aDiagram.getName(), "", aDiagram.getGUID(),
					diagramStatus.toString());
		} else {
			// Otherwise we determine if the diagram is a brand new one (i.e. doesn't exist in
			// the baseline)...
			if (preProcessor.getTargetNewDiagramsXmiIds().containsKey(aDiagram.getXmiId())) {
				DiagramProperties newDiagramProperties = new DiagramProperties(null, null, aDiagram,
						diagramParentPackage);
				Properties properties = newDiagramProperties.getProperties();

				diagramStatus = Status.ModelOnly;
				
				theDiagram = new CompareItem(properties, aDiagram.getName(), "Diagram", aDiagram.getGUID(),
						diagramStatus.toString());
			} else {
				Diagram baselineDiagram = (preProcessor.getBaselineDiagramsXmiIds().containsKey(aDiagram.getXmiId())
						? preProcessor.getBaselineDiagramsXmiIds().get(aDiagram.getXmiId())
						: null);

				PackageType baselineParentPackage = (baselineDiagram != null
						? preProcessor.getAllBaselinePackagesXmiIds().get(baselineDiagram.getOwner())
						: null);

				Diagram targetDiagram = (preProcessor.getTargetDiagramsXmiIds().containsKey(aDiagram.getXmiId())
						? preProcessor.getTargetDiagramsXmiIds().get(aDiagram.getXmiId())
						: null);
				
				PackageType targetParentPackage = (targetDiagram != null
						? preProcessor.getAllTargetPackagesXmiIds().get(targetDiagram.getOwner())
						: null);

				DiagramProperties diagramProperties = new DiagramProperties(baselineDiagram, baselineParentPackage,
						targetDiagram, targetParentPackage);
				Properties properties = diagramProperties.getProperties();

				if (preProcessor.getBaselineMovedDiagramsXmiIds().containsKey(aDiagram.getXmiId())
						|| preProcessor.getTargetMovedDiagramsXmiIds().containsKey(aDiagram.getXmiId())) {
					diagramStatus = Status.Moved;
				} else if ((baselineDiagram.getDiagramElement() != null && targetDiagram.getDiagramElement() != null)
						&& baselineDiagram.getDiagramElement().getDiagramElements().size() != targetDiagram
								.getDiagramElement().getDiagramElements().size()) {
					// If they are not the same size we can simply declare as "changed" and skip
					// the additional processing that appears in the else...
					diagramStatus = Status.Changed;
				} else {
					diagramStatus = Status.Identical;

					Comparator<DiagramElement> comparator = new Comparator<DiagramElement>() {
						public int compare(DiagramElement item1, DiagramElement item2) {
							if (item1 == item2) {
								return 0;
							}
							if (item1 == null) {
								return -1;
							}
							if (item2 == null) {
								return 1;
							}

							String item1Seqno = item1.getSeqno();
							String item2Seqno = item2.getSeqno();

							String item1Subject = item1.getSubject();
							String item2Subject = item2.getSubject();

							if (item1Seqno == null && item2Seqno == null) {
								if (item1Subject == null && item2Subject == null) {
									return 0;
								}
								if (item1Subject == null) {
									return -1;
								}
								if (item2Subject == null) {
									return 1;
								}
								return item1Subject.compareTo(item2Subject);
							} else {
								if (item1Seqno == null) {
									return -1;
								}
								if (item2Seqno == null) {
									return 1;
								}
								if ((item1Seqno.equals(item2Seqno))) {
									if (item1Subject == null && item2Subject == null) {
										return 0;
									}
									if (item1Subject == null) {
										return -1;
									}
									if (item2Subject == null) {
										return 1;
									}
									return item1Subject.compareTo(item2Subject);
								}
								return item1Seqno.compareTo(item2Seqno);
							}
						}
					};

					DiagramElementType baselineDiagramElementType = baselineDiagram.getDiagramElement();
					DiagramElementType targetDiagramElementType = targetDiagram.getDiagramElement();

					if ((baselineDiagramElementType != null && targetDiagramElementType != null)
							&& (baselineDiagramElementType.getDiagramElements().size() == targetDiagramElementType
									.getDiagramElements().size())) {

						List<DiagramElement> baselineDiagramElements = baselineDiagramElementType.getDiagramElements();
						baselineDiagramElements.sort(comparator);

						List<DiagramElement> targetDiagramElements = targetDiagramElementType.getDiagramElements();
						targetDiagramElements.sort(comparator);

						for (int index = 0; index < baselineDiagramElements.size(); index++) {
							DiagramElement baselineDiagramElement = baselineDiagramElements.get(index);
							DiagramElement targetDiagramElement = targetDiagramElements.get(index);

							if (!((baselineDiagramElement.getSeqno() == null && targetDiagramElement.getSeqno() == null)
									|| ((baselineDiagramElement.getSeqno() != null)
											&& (targetDiagramElement.getSeqno() != null) && baselineDiagramElement
													.getSeqno().equals(targetDiagramElement.getSeqno())))
									|| //
									!baselineDiagramElement.getGeometry().equals(targetDiagramElement.getGeometry()) || //
									!baselineDiagramElement.getStyle().equals(targetDiagramElement.getStyle()) || //
									!baselineDiagramElement.getSubject().equals(targetDiagramElement.getSubject())) {
								diagramStatus = Status.Changed;
								break; // Break out of the for loop as we know the diagram has changed...
							}
						}
					} else if ((baselineDiagramElementType == null && targetDiagramElementType != null)
							|| (baselineDiagramElementType != null && targetDiagramElementType == null)) {
						diagramStatus = Status.Changed;
					}
					
					if (Status.Identical.equals(diagramStatus)) {
						for (Property prop : properties.getProperty()) {
							if (!Status.Identical.toString().equals(prop.getStatus())) {
								diagramStatus = Status.Changed;
								break;
							}
						}
					}
				}

				theDiagram = new CompareItem(properties, targetDiagram.getName(), "Diagram", targetDiagram.getGUID(),
						diagramStatus.toString());
				
				if (Status.Identical.equals(diagramStatus)) {
					File baselineImageFile = new File(this.baselineImagesDir, targetDiagram.getXmiId()+ "." + imageType.ext());
					if (baselineImageFile.delete()) {
						System.out.println("Image file deleted: " + baselineImageFile.getAbsolutePath());
					};
					File destinationImageFile = new File(this.destinationImagesDir, targetDiagram.getXmiId()+ "." + imageType.ext());
					if (destinationImageFile.delete()) {
						System.out.println("Image file deleted: " + destinationImageFile.getAbsolutePath());
					}
				} 
			}
		}

		return theDiagram;
	}

	private CompareItem parseAttribute(AttributeType baselineAttribute, AttributeType targetAttribute) {

		CompareItem theAttribute;

		AttributeProperties attributeProperties = new AttributeProperties(baselineAttribute, targetAttribute);
		Properties properties = attributeProperties.getProperties();

		// It is assumed that both attributes will never be both null...
		if (baselineAttribute == null) {
			// Only model attribute exists...
			theAttribute = new CompareItem(properties, new ArrayList<CompareItem>(), targetAttribute.getName(),
					"Attribute", targetAttribute.getTheValue("ea_guid"), Status.ModelOnly.toString());
		} else if (targetAttribute == null) {
			// Only baseline attribute exists.
			// NOTE: This corresponds to a deleted attribute and therefore the type
			// parameter is specified
			// as an empty string (i.e. "")...
			theAttribute = new CompareItem(properties, new ArrayList<CompareItem>(), baselineAttribute.getName(), "",
					baselineAttribute.getTheValue("ea_guid"), Status.BaselineOnly.toString());
		} else {
			// Both attributes exist...
			theAttribute = new CompareItem(properties, new ArrayList<CompareItem>(), targetAttribute.getName(),
					"Attribute", targetAttribute.getTheValue("ea_guid"), attributeProperties.getStatus().toString());
		}

		return theAttribute;
	}

	private CompareItem parseLinks(ClassType baselineClass, PackageType baselineParentPackage, ClassType targetClass,
			PackageType targetParentPackage) {

		// NOTE: The status of the "Links" CompareItem is ALWAYS 'Identical'...
		CompareItem links = new CompareItem(null, "Links", "Links",
				(targetClass != null ? targetClass.getGUID() : baselineClass.getGUID()) + "Links",
				Status.Identical.toString());

		if (baselineClass == null) {
			// Only model class exists...

			for (GeneralizationType targetGeneralization : preProcessor
					.getAllTargetGeneralizations(targetClass.getXmiId())) {

				//
				// There exists the edge case where a generalization exists in both the
				// baseline and target models but in the target model an end of the
				// generalization is moved from one class to another. In that case the
				// generalization is NOT a "Model only" (i.e. new) generalization but
				// rather is an existing generalization which required that we obtain the
				// baseline generalization and class for processing...
				//
				String targetXmiId = targetGeneralization.getXmiId();
				GeneralizationType baselineGeneralization = null;

				if (preProcessor.getBaselineGeneralizationsXmiIds().containsKey(targetXmiId) || //
						preProcessor.getBaselineDeletedGeneralizationsXmiIds().containsKey(targetXmiId)) {
					baselineGeneralization = (preProcessor.getBaselineGeneralizationsXmiIds().containsKey(targetXmiId)
							? preProcessor.getBaselineGeneralizationsXmiIds().get(targetXmiId)
							: preProcessor.getBaselineDeletedGeneralizationsXmiIds().get(targetXmiId));
				}

				GeneralizationProperties propsProcessor = new GeneralizationProperties(preProcessor,
						baselineGeneralization, targetGeneralization);

				CompareItem generalizationCompareItem = new CompareItem(propsProcessor.getProperties(),
						"Generalization", "", targetGeneralization.getGUID(), propsProcessor.getStatus().toString());

				/** Source end... */
				CompareItem sourceCompareItem = new CompareItem(
						propsProcessor.getSourcePropsProcessor().getProperties(),
						"Source: (" + propsProcessor.getSourcePropsProcessor().getRoleName() + ")", "Src",
						"Src-" + targetGeneralization.getGUID(),
						propsProcessor.getSourcePropsProcessor().getStatus().toString());

				generalizationCompareItem.getCompareItem().add(sourceCompareItem);

				/** Destination end... */
				CompareItem destinationCompareItem = new CompareItem(
						propsProcessor.getDestinationPropsProcessor().getProperties(),
						"Target: (" + propsProcessor.getDestinationPropsProcessor().getRoleName() + ")", "Dst",
						"Dst-" + targetGeneralization.getGUID(),
						propsProcessor.getDestinationPropsProcessor().getStatus().toString());

				generalizationCompareItem.getCompareItem().add(destinationCompareItem);

				// Finally we add the new generalization compare item to the links
				links.getCompareItem().add(generalizationCompareItem);
			}

			for (AssociationType targetAssociation : preProcessor.getAllTargetAssociations(targetClass.getXmiId())) {

				AssociationProperties propsProcessor = new AssociationProperties(preProcessor, null, null, targetClass,
						targetAssociation);

				CompareItem associationCompareItem = new CompareItem(propsProcessor.getProperties(), "Association", "",
						targetAssociation.getGUID(), propsProcessor.getStatus().toString());

				/**
				 * Create 'Src' end of the association...
				 */
				String sourceGUID = generateSourceGUID(targetClass, targetAssociation,
						propsProcessor.getTargetSourceAssociationEnd());

				CompareItem sourceEndCompareItem = new CompareItem(
						propsProcessor.getSourcePropsProcessor().getProperties(), //
						(targetAssociation.getSourceAssociationEnd().getName() != null ? "Source: (" + targetAssociation.getSourceAssociationEnd().getName() + ")" : "Source"), //
						"Src", sourceGUID,
						propsProcessor.getSourcePropsProcessor().getStatus().toString());

				associationCompareItem.getCompareItem().add(sourceEndCompareItem);

				/**
				 * Create 'Dst' end of the association...
				 */
				String destinationGUID = generateDestinationGUID(targetClass, targetAssociation,
						propsProcessor.getTargetDestinationAssociationEnd());

				CompareItem destinationEndCompareItem = new CompareItem(
						propsProcessor.getDestinationPropsProcessor().getProperties(), //
						(targetAssociation.getDestinationAssociationEnd().getName() != null ? "Target: (" + targetAssociation.getDestinationAssociationEnd().getName() + ")" : "Target"), //
						"Dst",
						destinationGUID, propsProcessor.getDestinationPropsProcessor().getStatus().toString());

				associationCompareItem.getCompareItem().add(destinationEndCompareItem);

				// Finally we add the new association compare item to the links
				links.getCompareItem().add(associationCompareItem);
			}
		} else if (targetClass == null) {
			// Only baseline class exists...so we know the class has been deleted.

			for (GeneralizationType baselineGeneralization : preProcessor
					.getAllBaselineGeneralizations(baselineClass.getXmiId())) {

				//
				// There exists the edge case where a generalization exists in both the
				// baseline and target models but in the target model an end of the
				// generalization is moved from one class to another. In that case the
				// generalization is NOT a "Model only" (i.e. new) generalization but
				// rather is an existing generalization which required that we obtain the
				// baseline generalization and class for processing...
				//
				String baselineXmiId = baselineGeneralization.getXmiId();

				GeneralizationType targetGeneralization = null;

				if (preProcessor.getTargetGeneralizationsXmiIds().containsKey(baselineXmiId) || //
						preProcessor.getTargetNewGeneralizationsXmiIds().containsKey(baselineXmiId)) {
					targetGeneralization = (preProcessor.getTargetGeneralizationsXmiIds().containsKey(baselineXmiId)
							? preProcessor.getTargetGeneralizationsXmiIds().get(baselineXmiId)
							: preProcessor.getTargetNewGeneralizationsXmiIds().get(baselineXmiId));
				}

				GeneralizationProperties propsProcessor = new GeneralizationProperties(preProcessor,
						baselineGeneralization, targetGeneralization);

				CompareItem generalizationCompareItem = new CompareItem(propsProcessor.getProperties(),
						"Generalization", "", baselineGeneralization.getGUID(), propsProcessor.getStatus().toString());

				/** Source end... */
				CompareItem sourceCompareItem = new CompareItem(
						propsProcessor.getSourcePropsProcessor().getProperties(),
						"Source: (" + propsProcessor.getSourcePropsProcessor().getRoleName() + ")", "Src",
						"Src-" + baselineGeneralization.getGUID(),
						propsProcessor.getSourcePropsProcessor().getStatus().toString());

				generalizationCompareItem.getCompareItem().add(sourceCompareItem);

				/** Destination end... */
				CompareItem destinationCompareItem = new CompareItem(
						propsProcessor.getDestinationPropsProcessor().getProperties(),
						"Target: (" + propsProcessor.getDestinationPropsProcessor().getRoleName() + ")", "Dst",
						"Dst-" + baselineGeneralization.getGUID(),
						propsProcessor.getDestinationPropsProcessor().getStatus().toString());

				generalizationCompareItem.getCompareItem().add(destinationCompareItem);

				// Finally we add the new generalization compare item to the links
				links.getCompareItem().add(generalizationCompareItem);
			}

			for (AssociationType baselineAssociation : preProcessor
					.getAllBaselineAssociations(baselineClass.getXmiId())) {

				AssociationProperties propsProcessor = new AssociationProperties(preProcessor, baselineClass,
						baselineAssociation, null, null);

				CompareItem associationCompareItem = new CompareItem(propsProcessor.getProperties(), "Association", "",
						baselineAssociation.getGUID(), propsProcessor.getStatus().toString());

				/**
				 * Create 'Src' end of the association...
				 */
				String sourceGUID = generateSourceGUID(baselineClass, baselineAssociation,
						propsProcessor.getBaselineSourceAssociationEnd());

				CompareItem sourceEndCompareItem = new CompareItem(
						propsProcessor.getSourcePropsProcessor().getProperties(), //
						(baselineAssociation.getSourceAssociationEnd().getName() != null ? "Source: (" + baselineAssociation.getSourceAssociationEnd().getName() + ")" : "Source"), //
						"Src", //
						sourceGUID,
						propsProcessor.getSourcePropsProcessor().getStatus().toString());

				associationCompareItem.getCompareItem().add(sourceEndCompareItem);

				/**
				 * Create 'Dst' end of the association...
				 */
				String destinationGUID = generateDestinationGUID(baselineClass, baselineAssociation,
						propsProcessor.getBaselineDestinationAssociationEnd());

				CompareItem destinationEndCompareItem = new CompareItem(
						propsProcessor.getDestinationPropsProcessor().getProperties(), //
						(baselineAssociation.getDestinationAssociationEnd().getName() != null ? "Target: (" + baselineAssociation.getDestinationAssociationEnd().getName() + ")" : "Target"), //
						"Dst",//
						destinationGUID, propsProcessor.getDestinationPropsProcessor().getStatus().toString());

				associationCompareItem.getCompareItem().add(destinationEndCompareItem);

				// Finally we add the new association compare item to the links
				links.getCompareItem().add(associationCompareItem);
			}
		} else {
			// Both classes exist...
			// NOTE: For "root" classes there is NO parent package. So ensure it is != null

			for (GeneralizationType targetGeneralization : preProcessor
					.getAllTargetGeneralizations(targetClass.getXmiId())) {

				GeneralizationType baselineGeneralization = (preProcessor.getBaselineGeneralizationsXmiIds()
						.containsKey(targetGeneralization.getXmiId())
								? preProcessor.getBaselineGeneralizationsXmiIds().get(targetGeneralization.getXmiId())
								: null);

				GeneralizationProperties propsProcessor = new GeneralizationProperties(preProcessor,
						baselineGeneralization, targetGeneralization);

				CompareItem generalizationCompareItem = new CompareItem(propsProcessor.getProperties(),
						"Generalization", "", targetGeneralization.getGUID(), propsProcessor.getStatus().toString());

				CompareItem sourceCompareItem = new CompareItem(
						propsProcessor.getSourcePropsProcessor().getProperties(),
						"Source: (" + propsProcessor.getSourcePropsProcessor().getRoleName() + ")", "Src",
						"Src-" + targetGeneralization.getGUID(),
						propsProcessor.getSourcePropsProcessor().getStatus().toString());

				generalizationCompareItem.getCompareItem().add(sourceCompareItem);

				CompareItem destinationCompareItem = new CompareItem(
						propsProcessor.getDestinationPropsProcessor().getProperties(),
						"Target: (" + propsProcessor.getDestinationPropsProcessor().getRoleName() + ")", "Dst",
						"Dst-" + targetGeneralization.getGUID(),
						propsProcessor.getDestinationPropsProcessor().getStatus().toString());

				generalizationCompareItem.getCompareItem().add(destinationCompareItem);

				links.getCompareItem().add(generalizationCompareItem);
			}

			/**
			 * We ensure that we don't forget about generalizations that are ONLY in the
			 * baseline (i.e. have been deleted):
			 */
			for (String generalizationXmiId : preProcessor.getBaselineDeletedGeneralizationsXmiIds().keySet()) {
				GeneralizationType deletedBaselineGeneralization = preProcessor
						.getBaselineDeletedGeneralizationsXmiIds().get(generalizationXmiId);

				// If the below condition is true then we have a deleted generalization.
				if (deletedBaselineGeneralization.getSubtype().equals(baselineClass.getXmiId())
						|| deletedBaselineGeneralization.getSupertype().equals(baselineClass.getXmiId())) {

					GeneralizationProperties propsProcessor = new GeneralizationProperties(preProcessor,
							deletedBaselineGeneralization, null);

					CompareItem deletedGeneralizationCompareItem = new CompareItem(propsProcessor.getProperties(),
							"Generalization", "", deletedBaselineGeneralization.getGUID(),
							propsProcessor.getStatus().toString());

					CompareItem sourceCompareItem = new CompareItem(
							propsProcessor.getSourcePropsProcessor().getProperties(),
							"Source: (" + propsProcessor.getSourcePropsProcessor().getRoleName() + ")", "Src",
							"Src-" + deletedBaselineGeneralization.getGUID(),
							propsProcessor.getSourcePropsProcessor().getStatus().toString());

					deletedGeneralizationCompareItem.getCompareItem().add(sourceCompareItem);

					CompareItem destinationCompareItem = new CompareItem(
							propsProcessor.getDestinationPropsProcessor().getProperties(),
							"Target: (" + propsProcessor.getDestinationPropsProcessor().getRoleName() + ")", "Dst",
							"Dst-" + deletedBaselineGeneralization.getGUID(),
							propsProcessor.getDestinationPropsProcessor().getStatus().toString());

					deletedGeneralizationCompareItem.getCompareItem().add(destinationCompareItem);

					links.getCompareItem().add(deletedGeneralizationCompareItem);
				}
			}

			for (AssociationType targetAssociation : preProcessor.getAllTargetAssociations(targetClass.getXmiId())) {

				AssociationType baselineAssociation = (preProcessor.getBaselineAssociationsXmiIds()
						.containsKey(targetAssociation.getXmiId())
								? preProcessor.getBaselineAssociationsXmiIds().get(targetAssociation.getXmiId())
								: null);

				AssociationProperties propsProcessor = new AssociationProperties(preProcessor, baselineClass,
						baselineAssociation, targetClass, targetAssociation);
				//
				CompareItem associationCompareItem = new CompareItem(propsProcessor.getProperties(), "Association", "",
						targetAssociation.getGUID(), propsProcessor.getStatus().toString());

				String sourceGUID = generateSourceGUID(targetClass, targetAssociation,
						propsProcessor.getTargetSourceAssociationEnd());
				//
				CompareItem sourceEndCompareItem = new CompareItem(
						propsProcessor.getSourcePropsProcessor().getProperties(), //
						(targetAssociation.getSourceAssociationEnd().getName() != null ? "Source: (" + targetAssociation.getSourceAssociationEnd().getName() + ")" : "Source"), //
						"Src", //
						sourceGUID,
						propsProcessor.getSourcePropsProcessor().getStatus().toString());

				String destinationGUID = generateDestinationGUID(targetClass, targetAssociation,
						propsProcessor.getTargetDestinationAssociationEnd());
				//
				CompareItem destinationEndCompareItem = new CompareItem(
						propsProcessor.getDestinationPropsProcessor().getProperties(),//
						(targetAssociation.getDestinationAssociationEnd().getName() != null ? "Target: (" + targetAssociation.getDestinationAssociationEnd().getName() + ")" : "Target"), //
						"Dst", //
						destinationGUID, propsProcessor.getDestinationPropsProcessor().getStatus().toString());

				associationCompareItem.getCompareItem().add(sourceEndCompareItem);
				associationCompareItem.getCompareItem().add(destinationEndCompareItem);

				links.getCompareItem().add(associationCompareItem);
				
				// We ensure we handle all associations that have been "moved"...
				if (baselineAssociation != null
						&& preProcessor.getTargetMovedAssociationsXmiIds().containsKey(targetAssociation.getXmiId())) {
				}
			}

			/**
			 * We ensure that we don't forget about associations that are ONLY in the
			 * baseline (i.e. have been deleted):
			 */
			for (String associationXmiId : preProcessor.getBaselineDeletedAssociationsXmiIds().keySet()) {
				AssociationType deletedBaselineAssociation = preProcessor.getBaselineDeletedAssociationsXmiIds()
						.get(associationXmiId);

				// If the below condition is true then we have a deleted associations.
				if (deletedBaselineAssociation.getSourceAssociationEnd().getType().equals(baselineClass.getXmiId())
						|| deletedBaselineAssociation.getDestinationAssociationEnd().getType()
								.equals(baselineClass.getXmiId())) {

					AssociationProperties propsProcessor = new AssociationProperties(preProcessor, baselineClass,
							deletedBaselineAssociation, null, null);
					//
					CompareItem associationCompareItem = new CompareItem(propsProcessor.getProperties(), "Association",
							"", deletedBaselineAssociation.getGUID(), propsProcessor.getStatus().toString());

					String sourceGUID = generateSourceGUID(baselineClass, deletedBaselineAssociation,
							propsProcessor.getBaselineSourceAssociationEnd());
					//
					CompareItem sourceEndCompareItem = new CompareItem(
							propsProcessor.getSourcePropsProcessor().getProperties(), //
							(deletedBaselineAssociation.getSourceAssociationEnd().getName() != null ? "Source: (" + deletedBaselineAssociation.getSourceAssociationEnd().getName() + ")" : "Source"), //
							"Src", //
							sourceGUID, propsProcessor.getSourcePropsProcessor().getStatus().toString());

					String destinationGUID = generateDestinationGUID(baselineClass, deletedBaselineAssociation,
							propsProcessor.getBaselineDestinationAssociationEnd());
					//
					CompareItem destinationEndCompareItem = new CompareItem(
							propsProcessor.getDestinationPropsProcessor().getProperties(), //
							(deletedBaselineAssociation.getDestinationAssociationEnd().getName() != null ? "Target: (" + deletedBaselineAssociation.getDestinationAssociationEnd().getName() + ")" : "Target"), //
							"Dst", //
							destinationGUID,
							propsProcessor.getDestinationPropsProcessor().getStatus().toString());

					associationCompareItem.getCompareItem().add(sourceEndCompareItem);
					associationCompareItem.getCompareItem().add(destinationEndCompareItem);

					links.getCompareItem().add(associationCompareItem);
				}
			}

		}

		return links;

	}

	/**
	 * Format of the Source CompareItem GUID is: 'Src-' + GUID of the class +
	 * 'Links' + GUID of the source AssociationEnd + GUID of the Association
	 * 
	 * @param sourceClass
	 * @param sourceLink
	 * @param sourceAssocationEnd
	 * @return
	 */
	private String generateSourceGUID(ClassType sourceClass, GUIDIdentifier sourceLink,
			AssociationEndType sourceAssocationEnd) {
		return "Src-" + sourceClass.getGUID() + "Links" + DiffUtils.convertXmiIdToEAGUID(sourceAssocationEnd.getType())
				+ sourceLink.getGUID();
	}

	/**
	 * Format of the Destination CompareItem GUID is: 'Dst-' + GUID of the class +
	 * 'Links' + GUID of the target AssociationEnd + GUID of the Association
	 * 
	 * @param destinationClass
	 * @param destinationLink
	 * @param destinationAssocationEnd
	 * @return
	 */
	private String generateDestinationGUID(ClassType destinationClass, GUIDIdentifier destinationLink,
			AssociationEndType destinationAssocationEnd) {
		return "Dst-" + destinationClass.getGUID() + "Links"
				+ DiffUtils.convertXmiIdToEAGUID(destinationAssocationEnd.getType()) + destinationLink.getGUID();
	}

}
