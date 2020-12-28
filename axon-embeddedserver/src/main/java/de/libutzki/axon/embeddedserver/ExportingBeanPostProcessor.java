package de.libutzki.axon.embeddedserver;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * Dieser {@link BeanPostProcessor} scannt die Beans danach, ob sie mit {@link Exported} markiert sind oder ihr Name im
 * <i>exportedbeannames</i> Property enthalten ist. Falls dem so ist, so wird das Bean in den übergeordneten
 * Parent-Context mit übernommen.
 *
 * @see Exported
 */
@Component
class ExportingBeanPostProcessor implements BeanPostProcessor {

	private final ConfigurableListableBeanFactory beanFactory;

	ExportingBeanPostProcessor( final ConfigurableListableBeanFactory beanFactory ) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization( final Object bean, final String beanName ) {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization( final Object bean, final String beanName ) {
		if ( "embeddedServer".equals( beanName ) ) {
			pushBeanToParentApplicationContext( bean, beanName );
		}

		return bean;
	}

	private void pushBeanToParentApplicationContext( final Object bean, final String beanName ) {
		final ConfigurableListableBeanFactory parentBeanFactory = getParentBeanFactory( );
		if ( parentBeanFactory == null ) {
			throw new IllegalStateException( "The bean " + beanName + " cannot be pushed to the parent context as it does not exist." );
		}

		parentBeanFactory.registerSingleton( beanName, bean );
	}

	private ConfigurableListableBeanFactory getParentBeanFactory( ) {
		final BeanFactory parent = beanFactory.getParentBeanFactory( );
		return parent instanceof ConfigurableListableBeanFactory ? ( ConfigurableListableBeanFactory ) parent : null;
	}
}
