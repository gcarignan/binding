package com.netappsid.binding;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import com.jgoodies.binding.beans.PropertyAccessException;
import com.jgoodies.binding.beans.PropertyNotFoundException;
import com.jgoodies.binding.value.ValueModel;
import com.netappsid.binding.beans.Bean;
import com.netappsid.binding.beans.support.ChangeSupportFactory;
import com.netappsid.binding.state.StateModel;
import com.netappsid.observable.ObservableCollectionSupportFactory;
import com.netappsid.validate.Validate;

/**
 * @author Eric Belanger
 * @author NetAppsID Inc.
 */
@SuppressWarnings("serial")
public abstract class PresentationModel extends Bean
{
	private final ChangeSupportFactory changeSupportFactory;
	private final ObservableCollectionSupportFactory observableCollectionSupportFactory;
	private Class<?> beanClass;
	private PresentationModel parentModel;
	private Map<String, PresentationModel> subModels;

	public PresentationModel(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory)
	{
		super(changeSupportFactory);
		this.changeSupportFactory = changeSupportFactory;
		this.observableCollectionSupportFactory = observableCollectionSupportFactory;
	}

	/**
	 * <p>
	 * Returns the bean class stored. This class is used to do introspection when no bean is currently set and thus allows creation of ValueModels and
	 * sub-PresentationModels.
	 * </p>
	 * 
	 * @return the bean class stored used for introspection.
	 */
	public Class<?> getBeanClass()
	{
		return beanClass;
	}

	/**
	 * <p>
	 * Returns the parent <code>PresentationModel</code> if there is one.
	 * </p>
	 * 
	 * @return the parent <code>PresentationModel</code>
	 */
	public PresentationModel getParentModel()
	{
		return parentModel;
	}

	/**
	 * <p>
	 * Returns the top-most <code>PresentationModel</code> in this hierarchy of models.
	 * </p>
	 * 
	 * @return the top-most <code>PresentationModel</code>
	 */
	public PresentationModel getRootModel()
	{
		PresentationModel rootModel = this;

		while (rootModel.getParentModel() != null)
		{
			rootModel = rootModel.getParentModel();
		}

		return rootModel;
	}

	/**
	 * <p>
	 * Looks up and lazily created a <code>PresentationModel</code> that adapts the bound property value with the specified name.
	 * </p>
	 * 
	 * <p>
	 * Subsequent calls to this method with the same property name return the same PresentationModel.
	 * </p>
	 * 
	 * <p>
	 * It is possible to pass a complex property name path. In this case, <code>PresentationModel</code> will be created for each property in the path and the
	 * <code>PresentationModel</code> for the last property in the path will be returned. The property name delimiter is the character '.'.
	 * </p>
	 * 
	 * @param modelName
	 *            the name of the property to adapt or a complex property name path delimited by '.'
	 * @return a <code>PresentationModel</code> that adapts the specified property value.
	 * 
	 * @see PresentationModelFactory
	 */
	public PresentationModel getSubModel(String modelName)
	{
		if (modelName.contains("."))
		{
			final String propertyName = modelName.substring(0, modelName.indexOf('.'));
			final String subModelName = modelName.substring(modelName.indexOf('.') + 1);

			return getSubModel(propertyName).getSubModel(subModelName);
		}
		else
		{
			PresentationModel subModel = getSubModels().get(modelName);
			if (subModel == null)
			{
				subModel = PresentationModelFactory.createPresentationModel(this, modelName);

				getSubModels().put(modelName, subModel);
				getStateModel().link(subModel.getStateModel());
			}

			return subModel;
		}
	}

	/**
	 * <p>
	 * Removes a PropertyChangeListener from the list of bean listeners. This method should be used to remove PropertyChangeListeners that were registered for
	 * all bound properties of the target bean.
	 * </p>
	 * 
	 * <p>
	 * If listener is {@code null}, no exception is thrown and no action is performed.
	 * </p>
	 * 
	 * @param listener
	 *            the PropertyChangeListener to be removed
	 * 
	 * @see #addBeanPropertyChangeListener(PropertyChangeListener)
	 * @see #addBeanPropertyChangeListener(String, PropertyChangeListener)
	 * @see #removeBeanPropertyChangeListener(String, PropertyChangeListener)
	 * @see #getBeanPropertyChangeListeners()
	 */
	public abstract void removeBeanPropertyChangeListener(PropertyChangeListener listener);

	/**
	 * <p>
	 * Removes a PropertyChangeListener from the listener list for a specific property. This method should be used to remove PropertyChangeListeners that were
	 * registered for a specific bound property.
	 * </p>
	 * 
	 * <p>
	 * If listener is {@code null}, no exception is thrown and no action is performed.
	 * </p>
	 * 
	 * @param propertyName
	 *            a valid property name
	 * @param listener
	 *            the PropertyChangeListener to be removed
	 * 
	 * @see #addBeanPropertyChangeListener(String, PropertyChangeListener)
	 * @see #removeBeanPropertyChangeListener(PropertyChangeListener)
	 * @see #getBeanPropertyChangeListeners(String)
	 */
	public abstract void removeBeanPropertyChangeListener(String propertyName, PropertyChangeListener listener);

	/**
	 * <p>
	 * Removes the PropertyChangeHandler from the observed bean, if the bean is not {@code null} and if bean property changes are observed. Also removes all
	 * listeners from the bean that have been registered with {@code #addBeanPropertyChangeListener} before.
	 * </p>
	 * 
	 * <p>
	 * Setting the bean to null has side-effects, for example the adapter fires a change event for the bound property <em>bean</em> and other properties. And
	 * the value of ValueModel's vended by this adapter may change. However, typically this is fine and setting the bean to null is the first choice for
	 * removing the reference from the bean to the adapter. Another way to clear the reference from the target bean is to call
	 * <code>#releaseBeanListeners</code>; it has no side-effects.
	 * </p>
	 * 
	 * @see #setBean(Object)
	 */
	public abstract void releaseBeanListeners();

	/**
	 * <p>
	 * Returns the bean that holds the adapted properties. This bean is the bean channel's content.
	 * </p>
	 * 
	 * @return the bean that holds the adapted properties
	 * 
	 * @see #setBean(Object)
	 * @see #getBeanChannel()
	 */
	public abstract Object getBean();

	/**
	 * Returns the root model's bean.<br/>
	 * 
	 * @return
	 */
	public Object getRootBean()
	{
		return getRootModel().getBean();
	}

	/**
	 * <p>
	 * Sets a new bean as content of the bean channel. All adapted properties will reflect this change.
	 * </p>
	 * 
	 * @param newBean
	 *            the new bean
	 * 
	 * @see #getBean()
	 * @see #getBeanChannel()
	 */
	public abstract void setBean(Object newBean);

	/**
	 * <p>
	 * Returns the ValueModel that holds the bean that in turn holds the adapted properties. This bean channel is shared by the PropertyAdapters created by the
	 * factory methods <code>#getValueModel</code>.
	 * </p>
	 * 
	 * @return the ValueModel that holds the bean that in turn holds the adapted properties
	 * 
	 * @see #getBean()
	 * @see #setBean(Object)
	 */
	public abstract ValueModel getBeanChannel();

	/**
	 * <p>
	 * Returns the value of specified bean property, {@code null} if the current bean is {@code null}.
	 * </p>
	 * 
	 * <p>
	 * This operation is supported only for readable bean properties.
	 * </p>
	 * 
	 * @param propertyName
	 *            the name of the property to be read
	 * @return the value of the adapted bean property, null if the bean is null
	 * 
	 * @throws NullPointerException
	 *             if the property name is null
	 * @throws UnsupportedOperationException
	 *             if the property is write-only
	 * @throws PropertyNotFoundException
	 *             if the property could not be found
	 * @throws PropertyAccessException
	 *             if the value could not be read
	 */
	public abstract Object getValue(String propertyName);

	/**
	 * <p>
	 * Sets the given new value for the specified bean property. Does nothing if this adapter's bean is {@code null}.
	 * </p>
	 * 
	 * <p>
	 * Notifies the associated value change listeners if the bean reports a property change. Note that a bean may suppress PropertyChangeEvents if the old and
	 * new value are the same, or if the old and new value are equal.
	 * </p>
	 * 
	 * <p>
	 * This operation is supported only for writable bean properties.
	 * </p>
	 * 
	 * @param propertyName
	 *            the name of the property to set
	 * @param newValue
	 *            the value to set
	 * 
	 * @throws NullPointerException
	 *             if propertyName is null
	 * @throws UnsupportedOperationException
	 *             if the property is read-only
	 * @throws PropertyNotFoundException
	 *             if the property could not be found
	 * @throws PropertyAccessException
	 *             if the new value could not be set
	 */
	public abstract void setValue(String propertyName, Object newValue);

	/**
	 * <p>
	 * Looks up and lazily creates a ValueModel that adapts the bound property with the specified name. Uses the Bean introspection to look up the getter and
	 * setter names.
	 * </p>
	 * 
	 * <p>
	 * Subsequent calls to this method with the same property name return the same ValueModel.
	 * </p>
	 * 
	 * @param propertyName
	 *            the name of the property to adapt
	 * @return a ValueModel that adapts the property with the specified name
	 * 
	 * @throws NullPointerException
	 *             if propertyName is null
	 * @throws PropertyNotFoundException
	 *             if the property could not be found
	 * @throws IllegalArgumentException
	 *             if <code>#getValueModel(String, String, String)</code> has been called before with the same property name and a non-null getter or setter
	 *             name
	 */
	public abstract ValueModel getValueModel(String propertyName);

	public abstract StateModel getStateModel();

	/**
	 * <p>
	 * Returns the map used to stored the sub presentation models.
	 * </p>
	 * 
	 * <p>
	 * The map is lazily created the first time this method is called.
	 * </p>
	 * 
	 * @return the map of sub presentation models.
	 */
	protected Map<String, PresentationModel> getSubModels()
	{
		if (subModels == null)
		{
			subModels = new HashMap<String, PresentationModel>();
		}

		return subModels;
	}

	/**
	 * <p>
	 * Returns the bean class used for property introspection.
	 * </p>
	 * 
	 * @param beanClass
	 *            the bean class used for property introspection
	 */
	protected void setBeanClass(Class<?> beanClass)
	{
		this.beanClass = Validate.notNull(beanClass, "Bean class must not be null.");
	}

	/**
	 * <p>
	 * Sets the parent presentation model.
	 * </p>
	 * 
	 * @param parentModel
	 *            the parent presentation model
	 */
	protected void setParentModel(PresentationModel parentModel)
	{
		this.parentModel = parentModel;
	}

	public final ChangeSupportFactory getChangeSupportFactory()
	{
		return changeSupportFactory;
	}

	public final ObservableCollectionSupportFactory getObservableCollectionSupportFactory()
	{
		return observableCollectionSupportFactory;
	}

	public abstract void dispose();
}
