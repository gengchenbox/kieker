/**
 */
package kieker.model.analysismodel.statistics.util;

import java.util.Map;

import kieker.model.analysismodel.statistics.*;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see kieker.model.analysismodel.statistics.StatisticsPackage
 * @generated
 */
public class StatisticsAdapterFactory extends AdapterFactoryImpl {
	/**
	 * The cached model package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static StatisticsPackage modelPackage;

	/**
	 * Creates an instance of the adapter factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public StatisticsAdapterFactory() {
		if (modelPackage == null) {
			modelPackage = StatisticsPackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc -->
	 * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
	 * <!-- end-user-doc -->
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	@Override
	public boolean isFactoryForType(Object object) {
		if (object == modelPackage) {
			return true;
		}
		if (object instanceof EObject) {
			return ((EObject)object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch that delegates to the <code>createXXX</code> methods.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected StatisticsSwitch<Adapter> modelSwitch =
		new StatisticsSwitch<Adapter>() {
			@Override
			public Adapter caseStatisticRecord(StatisticRecord object) {
				return createStatisticRecordAdapter();
			}
			@Override
			public Adapter caseEPropertyTypeToValue(Map.Entry<String, Object> object) {
				return createEPropertyTypeToValueAdapter();
			}
			@Override
			public Adapter caseMeasurement(Measurement object) {
				return createMeasurementAdapter();
			}
			@Override
			public Adapter caseScalarMeasurement(ScalarMeasurement object) {
				return createScalarMeasurementAdapter();
			}
			@Override
			public Adapter caseVectorMeasurement(VectorMeasurement object) {
				return createVectorMeasurementAdapter();
			}
			@Override
			public Adapter caseIntMeasurement(IntMeasurement object) {
				return createIntMeasurementAdapter();
			}
			@Override
			public Adapter caseLongMeasurement(LongMeasurement object) {
				return createLongMeasurementAdapter();
			}
			@Override
			public Adapter caseFloatMeasurement(FloatMeasurement object) {
				return createFloatMeasurementAdapter();
			}
			@Override
			public Adapter caseDoubleMeasurement(DoubleMeasurement object) {
				return createDoubleMeasurementAdapter();
			}
			@Override
			public Adapter caseStatisticsModel(StatisticsModel object) {
				return createStatisticsModelAdapter();
			}
			@Override
			public Adapter caseEObjectToStatisticsMapEntry(Map.Entry<EObject, StatisticRecord> object) {
				return createEObjectToStatisticsMapEntryAdapter();
			}
			@Override
			public Adapter caseUnit(Unit object) {
				return createUnitAdapter();
			}
			@Override
			public Adapter caseComposedUnit(ComposedUnit object) {
				return createComposedUnitAdapter();
			}
			@Override
			public Adapter caseSimpleUnit(SimpleUnit object) {
				return createSimpleUnitAdapter();
			}
			@Override
			public Adapter caseSIUnit(SIUnit object) {
				return createSIUnitAdapter();
			}
			@Override
			public Adapter caseCustomUnit(CustomUnit object) {
				return createCustomUnitAdapter();
			}
			@Override
			public Adapter defaultCase(EObject object) {
				return createEObjectAdapter();
			}
		};

	/**
	 * Creates an adapter for the <code>target</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param target the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	@Override
	public Adapter createAdapter(Notifier target) {
		return modelSwitch.doSwitch((EObject)target);
	}


	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.StatisticRecord <em>Statistic Record</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.StatisticRecord
	 * @generated
	 */
	public Adapter createStatisticRecordAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link java.util.Map.Entry <em>EProperty Type To Value</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see java.util.Map.Entry
	 * @generated
	 */
	public Adapter createEPropertyTypeToValueAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.Measurement <em>Measurement</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.Measurement
	 * @generated
	 */
	public Adapter createMeasurementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.ScalarMeasurement <em>Scalar Measurement</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.ScalarMeasurement
	 * @generated
	 */
	public Adapter createScalarMeasurementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.VectorMeasurement <em>Vector Measurement</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.VectorMeasurement
	 * @generated
	 */
	public Adapter createVectorMeasurementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.IntMeasurement <em>Int Measurement</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.IntMeasurement
	 * @generated
	 */
	public Adapter createIntMeasurementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.LongMeasurement <em>Long Measurement</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.LongMeasurement
	 * @generated
	 */
	public Adapter createLongMeasurementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.FloatMeasurement <em>Float Measurement</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.FloatMeasurement
	 * @generated
	 */
	public Adapter createFloatMeasurementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.DoubleMeasurement <em>Double Measurement</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.DoubleMeasurement
	 * @generated
	 */
	public Adapter createDoubleMeasurementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.Unit <em>Unit</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.Unit
	 * @generated
	 */
	public Adapter createUnitAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.ComposedUnit <em>Composed Unit</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.ComposedUnit
	 * @generated
	 */
	public Adapter createComposedUnitAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.SimpleUnit <em>Simple Unit</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.SimpleUnit
	 * @generated
	 */
	public Adapter createSimpleUnitAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.SIUnit <em>SI Unit</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.SIUnit
	 * @generated
	 */
	public Adapter createSIUnitAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.CustomUnit <em>Custom Unit</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.CustomUnit
	 * @generated
	 */
	public Adapter createCustomUnitAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link kieker.model.analysismodel.statistics.StatisticsModel <em>Model</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see kieker.model.analysismodel.statistics.StatisticsModel
	 * @generated
	 */
	public Adapter createStatisticsModelAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link java.util.Map.Entry <em>EObject To Statistics Map Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see java.util.Map.Entry
	 * @generated
	 */
	public Adapter createEObjectToStatisticsMapEntryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for the default case.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter() {
		return null;
	}

} //StatisticsAdapterFactory
