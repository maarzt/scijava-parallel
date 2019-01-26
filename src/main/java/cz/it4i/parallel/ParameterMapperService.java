
package cz.it4i.parallel;

import org.scijava.plugin.SingletonService;
import org.scijava.service.SciJavaService;

public interface ParameterMapperService extends
	SingletonService<ParallelizationParadigmParameterMapper>, SciJavaService
{

	@Override
	default Class<ParallelizationParadigmParameterMapper> getPluginType() {
		return ParallelizationParadigmParameterMapper.class;
	}

}
