
package cz.it4i.parallel;

import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

@Plugin(type = Service.class)
public class DefaultParameterMapperService extends
	AbstractSingletonService<ParallelizationParadigmParameterMapper> implements
	ParameterMapperService
{

}
