package com.temenos.interaction.rimdsl;

/*
 * #%L
 * com.temenos.interaction.rimdsl.tests
 * %%
 * Copyright (C) 2012 - 2013 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.eclipse.xtext.generator.IFileSystemAccess;
import org.eclipse.xtext.generator.IGenerator;
import org.eclipse.xtext.generator.InMemoryFileSystemAccess;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.temenos.interaction.rimdsl.rim.DomainModel;
import com.temenos.interaction.rimdsl.rim.ResourceInteractionModel;

@InjectWith(RIMDslSpringPRDInjectorProvider.class)
@RunWith(XtextRunner.class)
public class GeneratorSpringPRDTest {

	@Inject
	IGenerator underTest;
	@Inject
	ParseHelper<DomainModel> parseHelper;

	private final static String LINE_SEP = System.getProperty("line.separator");

	private final static String SIMPLE_STATES_RIM = "" + "rim Simple {" + LINE_SEP + "	command GetEntity" + LINE_SEP
			+ "	command GetException" + LINE_SEP + "	command UpdateEntity" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntity" + LINE_SEP + "}" + LINE_SEP +

			"exception resource E {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: EXCEPTION" + LINE_SEP
			+ "	view: GetException" + LINE_SEP + "}" + LINE_SEP +

			"resource B {" + "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP + "	actions [ UpdateEntity ]"
			+ LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "";
	
	private final static String DYNAMIC_STATE_RIM = "" + "rim Dynamic {" + LINE_SEP + "	command GetEntity" + LINE_SEP
			+ "	command UpdateEntity" + LINE_SEP +
			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntity" + LINE_SEP
			+ "GET *-> locator findsource(\"{reference}\"){" + LINE_SEP + "parameters [ id = \"{id}\" ]" + LINE_SEP + "title: \"Who & Why\"" + LINE_SEP + "}"
			+ LINE_SEP + "}" + LINE_SEP
			+ "resource B {" + "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP + "	actions [ UpdateEntity ]"
			+ LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "";	

	private final static String SIMPLE_STATES_BEHAVIOUR = "       <bean id=\"A\" class=\"com.temenos.interaction.core.hypermedia.CollectionResourceState\">"
			+ LINE_SEP + "<constructor-arg name=\"entityName\" value=\"ENTITY\" />";
	private final static String INCOMPLETE_RIM = "" + "rim Test {" + LINE_SEP + "	command GetEntity" + LINE_SEP
			+ "	command Noop" + LINE_SEP + "}" + LINE_SEP + "";

	private static final String BEAN_ID_INITIAL_STATE = "initialState";
	private static final String BEAN_ID_COLLECTION_RESOURCE_STATE = "com.temenos.interaction.core.hypermedia.CollectionResourceState";
	private static final String BEAN_ID_RESOURCE_STATE = "com.temenos.interaction.core.hypermedia.ResourceState";
	private static final String FACTORY_BEAN = "com.temenos.interaction.springdsl.TransitionFactoryBean";
	private static final String RESOURCE_STATE = "com.temenos.interaction.core.hypermedia.ResourceState";
	private static final String ACTION = "com.temenos.interaction.core.hypermedia.Action";
	private static final String TRANSITION_FACTORY_BEAN = "com.temenos.interaction.springdsl.TransitionFactoryBean";

	private String beanId;
	private String beanClass;
	private String path;


	private enum PROCESSING_STATE {
		INIT, RESOURCE_STATE, ACTION, FACTORY_BEAN, COLLECTION_RESOURCE_STATE
	};

	private PROCESSING_STATE processState = PROCESSING_STATE.INIT;

	private final static String SINGLE_STATE_VIEW_COMMAND_ONLY_RIM = "" + "rim Test {" + LINE_SEP
			+ "	command GetEntity" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntity" + LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "";

	/**
	 * Test generate files for spring and dispatcher registration.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testGenerateFiles() throws Exception {
		DomainModel domainModel = parseHelper.parse(SINGLE_STATE_VIEW_COMMAND_ONLY_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		assertEquals(2, fsa.getAllFiles().size());
		assertTrue(allFiles.containsKey(IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml"));
		assertTrue(allFiles.containsKey(IFileSystemAccess.DEFAULT_OUTPUT + "META-INF/IRIS-Test.properties"));
	}

	/**
	 * Test generate files for spring and dispatcher registration.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testBeanMap() throws Exception {
		DomainModel domainModel = parseHelper.parse(SINGLE_STATE_VIEW_COMMAND_ONLY_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		String output = allFiles.get(IFileSystemAccess.DEFAULT_OUTPUT + "META-INF/IRIS-Test.properties").toString();
		assertTrue(output.contains("Test-A=GET /A"));
	}

	private final static String SINGLE_STATE_METHODS_COMMAND_ONLY_RIM = "" + "rim Test {" + LINE_SEP + 
			"	event GET {" + LINE_SEP + 
			"		method: GET" + LINE_SEP + 
			"	}" + LINE_SEP +
			"	event POST {" + LINE_SEP + 
			"		method: POST" + LINE_SEP + 
			"	}" + LINE_SEP +
			"	command GetEntity" + LINE_SEP +
			"	command CreateEntity" + LINE_SEP +
			"	initial resource A {" + LINE_SEP + 
			"		type: collection" + LINE_SEP + 
			"		entity: ENTITY" + LINE_SEP +
			"		commands [ GET: GetEntity, POST: CreateEntity ]" + LINE_SEP + 
			"	}" + LINE_SEP + 
			"}" + LINE_SEP + "";

	@Test
	public void testBeanMapMethods() throws Exception {
		DomainModel domainModel = parseHelper.parse(SINGLE_STATE_METHODS_COMMAND_ONLY_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		String output = allFiles.get(IFileSystemAccess.DEFAULT_OUTPUT + "META-INF/IRIS-Test.properties").toString();
		assertTrue(output.contains("Test-A=GET,POST /A"));
	}

	private final static String SINGLE_STATE_WITH_PACKAGE_RIM = "" + "domain blah {" + LINE_SEP + "rim Test {"
			+ LINE_SEP + "	command GetEntity" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntity" + LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "";

	/**
	 * Test generate file in package.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testGenerateFileInPackage() throws Exception {
		DomainModel domainModel = parseHelper.parse(SINGLE_STATE_WITH_PACKAGE_RIM);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(domainModel.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-blah_Test-PRD.xml";
		assertTrue(allFiles.containsKey(expectedKey));
		String output = allFiles.get(expectedKey).toString();
		assertTrue(output.contains("<bean id=\"blah_Test-A\" class=\"com.temenos.interaction.core.hypermedia.CollectionResourceState\">"));
		assertTrue(output.contains("<bean class=\"com.temenos.interaction.core.hypermedia.Action\">"));
		assertTrue(output.contains("<constructor-arg name=\"path\" value=\"/A\" />"));
		assertTrue(output.contains("<property name=\"transitions\">"));
	}

	/**
	 * Test generate single state view command only.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testGenerateSingleStateViewCommandOnly() throws Exception {
		DomainModel domainModel = parseHelper.parse(SINGLE_STATE_VIEW_COMMAND_ONLY_RIM);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(domainModel.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(allFiles.containsKey(expectedKey));
		String output = allFiles.get(expectedKey).toString();

		assertTrue(output.contains("<bean id=\"Test-A\" class=\"com.temenos.interaction.core.hypermedia.CollectionResourceState\">"));
		assertTrue(output.contains("<constructor-arg name=\"entityName\" value=\"ENTITY\" />"));
		assertTrue(output.contains("<constructor-arg name=\"name\" value=\"A\" />"));
		assertTrue(output.contains("<bean class=\"com.temenos.interaction.core.hypermedia.Action\">"));
		assertTrue(output.contains("<constructor-arg value=\"GetEntity\" />"));
		assertTrue(output.contains("<constructor-arg value=\"VIEW\" />"));
		assertTrue(output.contains("<constructor-arg name=\"path\" value=\"/A\" />"));
		assertTrue(output.contains("<property name=\"transitions\">"));
	}

	private final static String SINGLE_STATE_ACTION_COMMANDS_RIM = "" + "rim Test {" + LINE_SEP
			+ "	command GetEntity {" + LINE_SEP + "		properties [ getkey=getvalue ]" + LINE_SEP + "	}" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntity" + LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "";

	@Test
	public void testGenerateSingleStateActionCommands() throws Exception {
		DomainModel domainModel = parseHelper.parse(SINGLE_STATE_ACTION_COMMANDS_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(allFiles.containsKey(expectedKey));

		String output = allFiles.get(expectedKey).toString();
		String resourceA = getResourceBean(output, "Test-A");
		assertTrue(resourceA.contains("<bean id=\"Test-A\" class=\"com.temenos.interaction.core.hypermedia.CollectionResourceState\">"));
		assertTrue(resourceA.contains("<constructor-arg name=\"entityName\" value=\"ENTITY\" />"));
		assertTrue(resourceA.contains("<constructor-arg name=\"name\" value=\"A\" />"));
		assertTrue(resourceA.contains("<bean class=\"com.temenos.interaction.core.hypermedia.Action\">"));
		assertTrue(resourceA.contains("<constructor-arg value=\"GetEntity\" />"));
		assertTrue(resourceA.contains("<constructor-arg value=\"VIEW\" />"));
		assertTrue(resourceA.contains("<prop key=\"getkey\">getvalue</prop>"));
		assertTrue(resourceA.contains("constructor-arg name=\"path\" value=\"/A\" />"));
	}

	private final static String MULTIPLE_STATES_MULTIPLE_ACTION_COMMANDS_RIM = "" + "rim Test {" + LINE_SEP
			+ "	command DoStuff {" + LINE_SEP + "		properties [ key=value ]" + LINE_SEP + "	}" + LINE_SEP
			+ "	command DoSomeStuff {" + LINE_SEP + "		properties [ keyB=valueB ]" + LINE_SEP + "	}" + LINE_SEP
			+ "	command DoSomeMoreStuff {" + LINE_SEP + "		properties [ keyB0=valueB0, keyB1=valueB1 ]" + LINE_SEP
			+ "	}" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	actions [ DoStuff ]" + LINE_SEP + "}" + LINE_SEP +

			"initial resource B {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	actions [ DoSomeStuff, DoSomeMoreStuff ]" + LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "";

	@Test
	public void testGenerateMultipleStateMultipleActionCommands() throws Exception {
		DomainModel domainModel = parseHelper.parse(MULTIPLE_STATES_MULTIPLE_ACTION_COMMANDS_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		Set<String> keys = allFiles.keySet();

		String resouceKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getFiles().containsKey(resouceKey));
		String resourceA = fsa.getFiles().get(resouceKey).toString();
		assertTrue(resourceA.contains("<bean class=\"com.temenos.interaction.core.hypermedia.Action\">"));
		assertTrue(resourceA.contains("<constructor-arg value=\"DoStuff\" />"));
		assertTrue(resourceA.contains("<constructor-arg value=\"ENTRY\" />"));

		String resourceB = fsa.getFiles().get(resouceKey).toString();
		assertTrue(resourceB.contains("<bean class=\"com.temenos.interaction.core.hypermedia.Action\">"));
		assertTrue(resourceB.contains("constructor-arg value=\"DoSomeMoreStuff\" />"));
		assertTrue(resourceB.contains("<constructor-arg value=\"ENTRY\" />"));
		assertTrue(resourceB.contains("<constructor-arg name=\"path\" value=\"/B\" />"));
		assertTrue(resourceB.contains(""));
		assertTrue(resourceB.contains(""));
	}

	private final static String TRANSITION_WITH_EXPRESSION_RIM = "" + 
			"rim Test {" + LINE_SEP + 
			"	event GET {" + LINE_SEP + 
			"		method: GET" + LINE_SEP + 
			"	}" + LINE_SEP +

			"	command GetEntity" + LINE_SEP + 
			"	command GetEntities" + LINE_SEP + 
			"	command PutEntity" + LINE_SEP +

			"initial resource A {" + LINE_SEP + 
			"	type: collection" + LINE_SEP + 
			"	entity: ENTITY" + LINE_SEP + 
			"	view: GetEntities" + LINE_SEP + 
			"	GET -> B { condition: OK(C) }" + LINE_SEP + 
			"	GET -> C { condition: NOT_FOUND(B) }" + LINE_SEP + 
			"	GET -> D { condition: OK(A) && NOT_FOUND(B) }" + LINE_SEP + 
			"	GET +-> E { condition: OK(C) }" + LINE_SEP + 
			"}" + LINE_SEP +

			"resource B {" + 
			"	type: item" + LINE_SEP + 
			"	entity: ENTITY" + LINE_SEP + 
			"	view: GetEntity" + LINE_SEP + 
			"}" + LINE_SEP + 
			"resource C {" + 
			"	type: item" + LINE_SEP + 
			"	entity: ENTITY" + LINE_SEP + 
			"	view: GetEntity" + LINE_SEP + 
			"}" + LINE_SEP + 
			"resource D {" + 
			"	type: item" + LINE_SEP + 
			"	entity: ENTITY" + LINE_SEP + 
			"	view: GetEntity" + LINE_SEP + 
			"}" + LINE_SEP + 
			"resource E {" + 
			"	type: item" + LINE_SEP + 
			"	entity: ENTITY" + LINE_SEP + 
			"	view: GetEntity" + LINE_SEP + 
			"}" + LINE_SEP + 
			"}" + LINE_SEP + "";

	@Test
	public void testGenerateTransitionsWithExpressions() throws Exception {
		DomainModel domainModel = parseHelper.parse(TRANSITION_WITH_EXPRESSION_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getAllFiles().containsKey(expectedKey));
		String output = fsa.getAllFiles().get(expectedKey).toString();

		// find the first resource (Test_A)
		String resourceA = getResourceBean(output, "Test-A");
		
		// find the transition to B, depends on C being found
		String transitionB = getTransitionBean(resourceA, "Test-B");
		assertTrue(transitionB.contains("<constructor-arg name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyResourceState\"><constructor-arg name=\"name\" value=\"Test-C\" /></bean></constructor-arg>"));
		assertTrue(transitionB.contains("<constructor-arg name=\"function\"><util:constant static-field=\"com.temenos.interaction.core.hypermedia.expression.ResourceGETExpression.Function.OK\"/>"));
		
		// find the transition to C, depends on B not being found
		String transitionC = getTransitionBean(resourceA, "Test-C");
		assertTrue(transitionC.contains("<constructor-arg name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyResourceState\"><constructor-arg name=\"name\" value=\"Test-B\" /></bean></constructor-arg>"));
		assertTrue(transitionC.contains("<constructor-arg name=\"function\"><util:constant static-field=\"com.temenos.interaction.core.hypermedia.expression.ResourceGETExpression.Function.NOT_FOUND\"/>"));

		// find the transition to D, depends on A being found and B not being found
		String transitionD = getTransitionBean(resourceA, "Test-D");
		assertTrue(transitionD.contains("<constructor-arg name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyCollectionResourceState\"><constructor-arg name=\"name\" value=\"Test-A\" /></bean></constructor-arg>"));
		assertTrue(transitionD.contains("<constructor-arg name=\"function\"><util:constant static-field=\"com.temenos.interaction.core.hypermedia.expression.ResourceGETExpression.Function.OK\"/>"));
		assertTrue(transitionD.contains("<constructor-arg name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyResourceState\"><constructor-arg name=\"name\" value=\"Test-B\" /></bean></constructor-arg>"));
		assertTrue(transitionD.contains("<constructor-arg name=\"function\"><util:constant static-field=\"com.temenos.interaction.core.hypermedia.expression.ResourceGETExpression.Function.NOT_FOUND\"/>"));

		// find the embedded transition to E, depends on C being found
		String transitionE = getTransitionBean(resourceA, "Test-E");
		assertTrue(transitionE.contains("<constructor-arg name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyResourceState\"><constructor-arg name=\"name\" value=\"Test-C\" /></bean></constructor-arg>"));
		assertTrue(transitionE.contains("<constructor-arg name=\"function\"><util:constant static-field=\"com.temenos.interaction.core.hypermedia.expression.ResourceGETExpression.Function.OK\"/>"));

	}

	private String getResourceBean(String output, String name) {
		String begin = "<!-- begin spring bean for state : " + name;
		int beginIndex = output.indexOf(begin);
		String end = "<!-- end spring bean for state : " + name;
		int endIndex = output.indexOf(end, beginIndex);
		return output.substring(beginIndex, endIndex);
	}

	private String getTransitionBean(String output, String name) {
		String begin = "<!-- begin transition : " + name;
		int beginIndex = output.indexOf(begin);
		String end = "<!-- end transition : " + name;
		int endIndex = output.indexOf(end, beginIndex);
		return output.substring(beginIndex, endIndex);
	}

	private final static String TRANSITION_WITH_MISSING_TARGET_RIM = "" + "rim Test {" + LINE_SEP + "	event GET {"
			+ LINE_SEP + "		method: GET" + LINE_SEP + "	}" + LINE_SEP +

			"	command GetEntities" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntities" + LINE_SEP + "	GET -> B" + LINE_SEP + "}" + LINE_SEP +

			"}" + LINE_SEP + "";

	@Test
	public void testGenerateTransitionsWithMissingTarget() throws Exception {
		DomainModel domainModel = parseHelper.parse(TRANSITION_WITH_MISSING_TARGET_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		Set<String> keys = allFiles.keySet();
		
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getFiles().containsKey(expectedKey));
		String output = fsa.getFiles().get(expectedKey).toString();

		// should not be adding the transition to a broken / missing state
		assertFalse(output.contains("<bean class=\"com.temenos.interaction.springdsl.TransitionFactoryBean\">"));
		assertFalse(output.contains("<property name=\"method\" value=\"GET\" />"));
		assertFalse(output.contains("<property name=\"target\" ref=\"B\" />"));
		assertFalse(output.contains("<property name=\"uriParameters\" ref=\"uriLinkageMap\" />"));
		assertFalse(output.contains("<property name=\"evaluation\" ref=\"conditionalLinkExpressions\" />"));
		assertFalse(output.contains("<property name=\"label\" value=\"B\" />"));		
	}

	private final static String TRANSITION_WITH_STRING_TARGET_RIM = "" + 
	"rim Test {" + LINE_SEP + 
	"	event GET {" + LINE_SEP + 
	"		method: GET" + LINE_SEP + 
	"	}" + LINE_SEP +
	"	command GetEntities" + LINE_SEP +
	"	initial resource A {" + LINE_SEP + 
	"		type: collection" + LINE_SEP + 
	"		entity: ENTITY" + LINE_SEP + 
	"		view: GetEntities" + LINE_SEP + 
	"		GET -> \"B\"" + LINE_SEP + 
	"	}" + LINE_SEP +
	"}" + LINE_SEP + "";

	@Test
	public void testGenerateTransitionsWithStringTarget() throws Exception {
		DomainModel domainModel = parseHelper.parse(TRANSITION_WITH_STRING_TARGET_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(allFiles.containsKey(expectedKey));
		String output = allFiles.get(expectedKey).toString();
		
		output = removeFormatting(output);

		// should find the transition to state
		assertTrue(output.contains("<bean class=\"com.temenos.interaction.springdsl.TransitionFactoryBean\">"));
		assertTrue(output.contains("<property name=\"method\" value=\"GET\" />"));
		assertTrue(output.contains("<property name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyResourceState\"><constructor-arg name=\"name\" value=\"B\" /></bean></property>"));
		assertTrue(output.contains("<property name=\"uriParameters\"><util:map></util:map></property>"));
		assertTrue(output.contains("<property name=\"evaluation\"><null /></property>"));
		assertTrue(output.contains("<property name=\"label\" value=\"B\" />"));
	}

	private final static String AUTO_TRANSITION_WITH_URI_LINKAGE_RIM = "" + "rim Test {" + LINE_SEP + "	event GET {"
			+ LINE_SEP + "		method: GET" + LINE_SEP + "	}" + LINE_SEP +

			"	command GetEntity" + LINE_SEP + "	command GetEntities" + LINE_SEP + "	command CreateEntity" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntities" + LINE_SEP + "	POST -> create_pseudo_state" + LINE_SEP + "}" + LINE_SEP +

			"resource create_pseudo_state {" + "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	actions [ CreateEntity ]" + LINE_SEP + "   GET --> created { parameters [ id=\"{MyId}\" ] }" + LINE_SEP
			+ "}" + LINE_SEP + "resource created {" + "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntity" + LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "";

	@Test
	public void testGenerateAutoTransitionsWithUriLinkage() throws Exception {
		DomainModel domainModel = parseHelper.parse(AUTO_TRANSITION_WITH_URI_LINKAGE_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		Set<String> keys = allFiles.keySet();

		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getFiles().containsKey(expectedKey));
		String output = fsa.getFiles().get(expectedKey).toString();
		
		output = removeFormatting(output);

		assertTrue(output.contains("<bean class=\"com.temenos.interaction.springdsl.TransitionFactoryBean\">"));
		assertTrue(output.contains("<property name=\"flags\"><util:constant static-field=\"com.temenos.interaction.core.hypermedia.Transition.AUTO\"/></property>"));
		assertTrue(output.contains("<property name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyResourceState\"><constructor-arg name=\"name\" value=\"Test-created\" /></bean></property>"));
		assertTrue(output.contains("<property name=\"uriParameters\"><util:map>"));
		assertTrue(output.contains("<entry key=\"id\" value=\"{MyId}\"/>"));
		assertTrue(output.contains("</util:map>"));
	}

	private final static String REDIRECT_TRANSITION_WITH_URI_LINKAGE_RIM = "" + "rim Test {" + LINE_SEP
			+ "	event GET {" + LINE_SEP + "		method: GET" + LINE_SEP + "	}" + LINE_SEP +

			"	command GetEntity" + LINE_SEP + "	command GetEntities" + LINE_SEP + "	command DeleteEntity" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntities" + LINE_SEP + "	DELETE -> delete_pseudo_state" + LINE_SEP + "}" + LINE_SEP +

			"resource delete_pseudo_state {" + "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	actions [ DeleteEntity ]" + LINE_SEP + "   GET ->> deleted { parameters [ id=\"{MyId}\" ] }" + LINE_SEP
			+ "}" + LINE_SEP + "resource deleted {" + "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntity" + LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "";

	@Test
	public void testGenerateRedirectTransitionsWithUriLinkage() throws Exception {
		DomainModel domainModel = parseHelper.parse(REDIRECT_TRANSITION_WITH_URI_LINKAGE_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		Set<String> keys = allFiles.keySet();

		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getFiles().containsKey(expectedKey));
		String output = fsa.getFiles().get(expectedKey).toString();
		
		output = removeFormatting(output);

		assertTrue(output.contains("<bean class=\"com.temenos.interaction.springdsl.TransitionFactoryBean\">"));
		assertTrue(output.contains("<property name=\"flags\"><util:constant static-field=\"com.temenos.interaction.core.hypermedia.Transition.REDIRECT\"/></property>"));
		assertTrue(output.contains("<property name=\"method\" value=\"GET\" />"));
		assertTrue(output.contains("<property name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyResourceState\"><constructor-arg name=\"name\" value=\"Test-deleted\" /></bean></property>"));
		assertTrue(output.contains("<property name=\"uriParameters\"><util:map>"));
		assertTrue(output.contains("<entry key=\"id\" value=\"{MyId}\"/>"));

	}

	private final static String EMBEDDED_TRANSITION_RIM = "" + "rim Test {" + LINE_SEP + "	event GET {" + LINE_SEP
			+ "		method: GET" + LINE_SEP + "	}" + LINE_SEP +

			"	command GetEntity" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntity" + LINE_SEP + "	GET +-> B" + LINE_SEP + "}" + LINE_SEP +

			"resource B {" + "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP + "	view: GetEntity" + LINE_SEP
			+ "}" + LINE_SEP + "}" + LINE_SEP + "";

	@Test
	public void testGenerateEmbeddedTransitions() throws Exception {
		DomainModel domainModel = parseHelper.parse(EMBEDDED_TRANSITION_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(allFiles.containsKey(expectedKey));
		String output = allFiles.get(expectedKey).toString();
		
		output = removeFormatting(output);

		assertTrue(output.contains("<bean class=\"com.temenos.interaction.springdsl.TransitionFactoryBean\">"));
		assertTrue(output.contains("<property name=\"flags\"><util:constant static-field=\"com.temenos.interaction.core.hypermedia.Transition.EMBEDDED\"/></property>"));
		assertTrue(output.contains("<property name=\"method\" value=\"GET\" />"));
		assertTrue(output.contains("<property name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyResourceState\"><constructor-arg name=\"name\" value=\"Test-B\" /></bean></property>"));
		assertTrue(output.contains("<property name=\"uriParameters\"><util:map></util:map></property>"));
		assertTrue(output.contains("<property name=\"label\" value=\"B\" />"));


		String expectedBKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS--PRD.xml";
		assertTrue(output.contains("Test-B"));
	}

	private final static String RESOURCE_RELATIONS_RIM = "" + "rim Test {" + LINE_SEP + "	command Noop" + LINE_SEP
			+ "	command Update" + LINE_SEP +

			"initial resource accTransactions {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY"
			+ LINE_SEP + "   view: Noop" + LINE_SEP
			+ "   relations [ \"archives\", \"http://www.temenos.com/statement-entries\" ]" + LINE_SEP
			+ "   GET -> accTransaction" + LINE_SEP + "}\r\n" + LINE_SEP + "resource accTransaction {" + LINE_SEP
			+ "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP + "   actions [ Update ]" + LINE_SEP
			+ "   relations [ \"edit\" ]" + LINE_SEP + "}\r\n" + LINE_SEP + "}" + LINE_SEP + "";

	@Test
	public void testGenerateResourcesWithRelations() throws Exception {
		DomainModel domainModel = parseHelper.parse(RESOURCE_RELATIONS_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		// collection
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getAllFiles().containsKey(expectedKey));
		String accTransactionsOutput = fsa.getAllFiles().get(expectedKey).toString();
	
		/*
		 * <constructor-arg name="rels">
		 *   	<array>
		 *      	<value>archives</value>
		 *      	<value>http://www.temenos.com/statement-entries</value>
		 *   	</array>
         * </constructor-arg>
		 */

		// the constructor part
		assertTrue(accTransactionsOutput.contains("<constructor-arg name=\"rels\">"));
		assertTrue(accTransactionsOutput.contains("<value><![CDATA[archives]]></value>"));
		assertTrue(accTransactionsOutput.contains("<value><![CDATA[http://www.temenos.com/statement-entries]]></value>"));
		assertTrue(accTransactionsOutput.contains("<constructor-arg name=\"uriSpec\"><null /></constructor-arg>"));
		assertTrue(accTransactionsOutput.contains("<constructor-arg name=\"errorState\"><null /></constructor-arg>"));

		// item
		expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getAllFiles().containsKey(expectedKey));
		String accTransactionOutput = fsa.getAllFiles().get(expectedKey).toString();

		/*
		 * <constructor-arg name="rels">
		 *   	<array>
		 *      	<value>edit</value>
		 *   	</array>
         * </constructor-arg>
		 */

		// the constructor part
		assertTrue(accTransactionOutput.contains("<constructor-arg name=\"rels\">"));
		assertTrue(accTransactionOutput.contains("<value><![CDATA[edit]]></value>"));
	}

	private final static String GLOBAL_RESOURCE_RELATIONS_RIM = "" + "rim Test {" + LINE_SEP + 
			"	command Noop" + LINE_SEP + 
			"	command Update" + LINE_SEP +
			"	relation archiveRel {" + LINE_SEP + 
			"		fqn: \"archive\"" + LINE_SEP + 
			"	}" + LINE_SEP +
			"	relation editRel {" + LINE_SEP + 
			"		fqn: \"edit\"" + LINE_SEP
			+ "		description: \"See 'edit' in http://www.iana.org/assignments/link-relations/link-relations.xhtml\"" + LINE_SEP + 
			"	}" + LINE_SEP +

			"initial resource accTransactions {" + LINE_SEP + 
			"	type: collection" + LINE_SEP + 
			"	entity: ENTITY" + LINE_SEP + 
			"   view: Noop" + LINE_SEP + 
			"   relations [ archiveRel, \"http://www.temenos.com/statement-entries\" ]" + LINE_SEP + 
			"   PUT -> accTransaction" + LINE_SEP + 
			"}\r\n" + LINE_SEP + 
			"resource accTransaction {" + LINE_SEP + 
			"	type: item" + LINE_SEP + 
			"	entity: ENTITY" + LINE_SEP + 
			"   actions [ Update ]" + LINE_SEP + 
			"   relations [ editRel ]" + LINE_SEP + "}\r\n" + LINE_SEP + "}" + LINE_SEP + // end
																							// rim
			"";

	@Test
	public void testGenerateResourcesWithGlobalRelations() throws Exception {
		DomainModel domainModel = parseHelper.parse(GLOBAL_RESOURCE_RELATIONS_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		// collection
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getAllFiles().containsKey(expectedKey));
		String accTransactionsOutput = fsa.getAllFiles().get(expectedKey).toString();
	
		/*
		 * <constructor-arg name="rels">
		 *   	<array>
		 *      	<value>archives</value>
		 *      	<value>http://www.temenos.com/statement-entries</value>
		 *   	</array>
         * </constructor-arg>
		 */

		// the constructor part
		assertTrue(accTransactionsOutput.contains("<constructor-arg name=\"rels\">"));
		assertTrue(accTransactionsOutput.contains("<value><![CDATA[archive]]></value>"));
		assertTrue(accTransactionsOutput.contains("<value><![CDATA[http://www.temenos.com/statement-entries]]></value>"));
		assertTrue(accTransactionsOutput.contains("<constructor-arg name=\"uriSpec\"><null /></constructor-arg>"));
		assertTrue(accTransactionsOutput.contains("<constructor-arg name=\"errorState\"><null /></constructor-arg>"));

		// item
		expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getAllFiles().containsKey(expectedKey));
		String accTransactionOutput = fsa.getAllFiles().get(expectedKey).toString();

		/*
		 * <constructor-arg name="rels">
		 *   	<array>
		 *      	<value>edit</value>
		 *   	</array>
         * </constructor-arg>
		 */

		// the constructor part
		assertTrue(accTransactionOutput.contains("<constructor-arg name=\"rels\">"));
		assertTrue(accTransactionOutput.contains("<value><![CDATA[edit]]></value>"));
	}

	private final static String TRANSITION_WITH_UPDATE_EVENT = "" + "rim Test {" + LINE_SEP + "	event GET {" + LINE_SEP
			+ "		method: GET" + LINE_SEP + "	}" + LINE_SEP + "	event UPDATE {" + LINE_SEP + "		method: PUT" + LINE_SEP
			+ "	}" + LINE_SEP +

			"	command GetEntities" + LINE_SEP + "	command GetEntity" + LINE_SEP + "	command PutEntity" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntities" + LINE_SEP + "	GET *-> B" + LINE_SEP + "}" + LINE_SEP +

			"resource B {" + "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP + "	view: GetEntity" + LINE_SEP
			+ "	UPDATE -> B_pseudo" + LINE_SEP + "}" + LINE_SEP +

			"resource B_pseudo {" + "	type: item" + LINE_SEP + "	entity: ENTITY" + LINE_SEP + "	actions [ PutEntity ]"
			+ LINE_SEP + "	GET --> A { condition: NOT_FOUND(B) }" + LINE_SEP + "	GET --> B { condition: OK(B) }"
			+ LINE_SEP + "}" + LINE_SEP +

			"}" + LINE_SEP + "";

	String removeFormatting(String input) {
		return input.replaceAll("[\\n\\r\\t]+", "");
	}
	
	@Test
	public void testGenerateUpdateTransition() throws Exception {
		DomainModel domainModel = parseHelper.parse(TRANSITION_WITH_UPDATE_EVENT);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		Set<String> keys = allFiles.keySet();
		
		String resourceKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getFiles().containsKey(resourceKey));
		String resource = fsa.getFiles().get(resourceKey).toString();
		
		resource = removeFormatting(resource);
		
		assertTrue(resource.contains("<bean class=\"com.temenos.interaction.springdsl.TransitionFactoryBean\">"));
		assertTrue(resource.contains("<property name=\"flags\"><util:constant static-field=\"com.temenos.interaction.core.hypermedia.Transition.FOR_EACH\"/></property>"));
		assertTrue(resource.contains("<property name=\"method\" value=\"GET\" />"));				
		assertTrue(resource.contains("<property name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyResourceState\"><constructor-arg name=\"name\" value=\"Test-B\" /></bean></property>"));				
		assertTrue(resource.contains("<property name=\"uriParameters\"><util:map></util:map></property>"));
		assertTrue(resource.contains("<property name=\"label\" value=\"B\" />"));
		
		assertTrue(resource.contains("<bean class=\"com.temenos.interaction.springdsl.TransitionFactoryBean\">"));
		assertTrue(resource.contains("<property name=\"method\" value=\"PUT\" />"));
		assertTrue(resource.contains("<property name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyResourceState\"><constructor-arg name=\"name\" value=\"Test-B_pseudo\" /></bean></property>"));
		assertTrue(resource.contains("<property name=\"uriParameters\"><util:map></util:map></property>"));
		assertTrue(resource.contains("<property name=\"evaluation\"><null /></property>"));
		assertTrue(resource.contains("<property name=\"label\" value=\"B_pseudo\" />"));
			
		assertTrue(resource.contains("<bean class=\"com.temenos.interaction.springdsl.TransitionFactoryBean\">"));
		assertTrue(resource.contains("<property name=\"flags\"><util:constant static-field=\"com.temenos.interaction.core.hypermedia.Transition.AUTO\"/></property>"));
		assertTrue(resource.contains("<property name=\"target\"><bean class=\"com.temenos.interaction.core.hypermedia.LazyCollectionResourceState\"><constructor-arg name=\"name\" value=\"Test-A\" /></bean></property>"));
		assertTrue(resource.contains("<property name=\"uriParameters\"><util:map>"));
		assertTrue(resource.contains("</util:map>"));
		assertTrue(resource.contains("<property name=\"evaluation\">"));
		assertTrue(resource.contains(""));	
	}

	private final static String RESOURCE_ON_ERROR = "" + "rim Test {" + LINE_SEP + "	command GetEntity" + LINE_SEP
			+ "	command Noop" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntity" + LINE_SEP + "	onerror --> AE" + LINE_SEP + "}" + LINE_SEP +

			"resource AE {" + LINE_SEP + "	type: item" + LINE_SEP + "	entity: ERROR" + LINE_SEP + "	view: Noop"
			+ LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "";

	/**
	 * Test generate on error resource.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testGenerateOnErrorResource() throws Exception {
		DomainModel domainModel = parseHelper.parse(RESOURCE_ON_ERROR);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		Set<String> keys = allFiles.keySet();
		
		// collection
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getAllFiles().containsKey(expectedKey));
		assertTrue(fsa.getAllFiles().containsKey(IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml"));
		String output = fsa.getAllFiles().get(expectedKey).toString();
		assertTrue(output
				.contains("<bean id=\"Test-AE\" class=\"com.temenos.interaction.core.hypermedia.ResourceState\">"));
		assertTrue(output.contains("<constructor-arg name=\"entityName\" value=\"ERROR\" />"));
		assertTrue(output.contains("<constructor-arg name=\"name\" value=\"AE\" />"));
		assertTrue(output.contains("<bean class=\"com.temenos.interaction.core.hypermedia.Action\">"));
		assertTrue(output.contains("<constructor-arg value=\"Noop\" />"));
		assertTrue(output.contains("<constructor-arg value=\"VIEW\" />"));
		assertTrue(output.contains("<constructor-arg name=\"path\" value=\"/AE\" />"));
	}

	private final static String RESOURCE_ON_ERROR_SEPARATE_RIM = "" + 
			"domain ErrorTest {" + LINE_SEP + 
			"rim Test {" + LINE_SEP + 
			"	command GetEntity" + LINE_SEP + 
			"initial resource A {" + LINE_SEP + 
			"	type: collection" + LINE_SEP + 
			"	entity: ENTITY" + LINE_SEP + 
			"	view: GetEntity" + LINE_SEP + 
			"	onerror --> Error.AE" + LINE_SEP + 
			"}" + LINE_SEP + "}" + LINE_SEP	+ 
			// end Test rim

			"rim Error {" + LINE_SEP + 
			"	command Noop" + LINE_SEP + 
			"resource AE {" + LINE_SEP + 
			"	type: item"	+ LINE_SEP + 
			"	entity: ERROR" + LINE_SEP + 
			"	view: Noop" + LINE_SEP + 
			"}" + LINE_SEP + 
			"}" + LINE_SEP + 
			// end

			"}" + LINE_SEP + "";

	/**
	 * Test generate on error resource separate rim.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testGenerateOnErrorResourceSeparateRIM() throws Exception {
		DomainModel domainModel = parseHelper.parse(RESOURCE_ON_ERROR_SEPARATE_RIM);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(domainModel.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		
		// collection
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-ErrorTest_Test-PRD.xml";
		assertTrue(allFiles.containsKey(expectedKey));

		String output = allFiles.get(expectedKey).toString();
		assertTrue(output.contains("<bean id=\"ErrorTest_Test-A\" class=\"com.temenos.interaction.core.hypermedia.CollectionResourceState\">"));
		assertTrue(output.contains("<constructor-arg name=\"entityName\" value=\"ENTITY\" />"));
		assertTrue(output.contains("<constructor-arg name=\"name\" value=\"A\" />"));
		assertTrue(output.contains("<bean class=\"com.temenos.interaction.core.hypermedia.Action\">"));
		assertTrue(output.contains("<constructor-arg value=\"GetEntity\" />"));
		assertTrue(output.contains("<constructor-arg value=\"VIEW\" />"));
		assertTrue(output.contains("<constructor-arg name=\"path\" value=\"/A\" />"));
	}

	private final static String RESOURCE_WITH_BASEPATH = "" + "rim Test {" + LINE_SEP + "	command GetEntity" + LINE_SEP
			+ "	command Noop" + LINE_SEP + "	basepath: \"/{companyid}\"" + LINE_SEP +

			"initial resource A {" + LINE_SEP + "	type: collection" + LINE_SEP + "	entity: ENTITY" + LINE_SEP
			+ "	view: GetEntity" + LINE_SEP + "	path: \"/A\"" + LINE_SEP + "}" + LINE_SEP +

			"}" + LINE_SEP + "";

	/**
	 * Test generate resource with basepath.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testGenerateResourceWithBasepath() throws Exception {
		DomainModel domainModel = parseHelper.parse(RESOURCE_WITH_BASEPATH);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);

		Map<String, Object> allFiles = fsa.getAllFiles();
		Set<String> keys = allFiles.keySet();
		
		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Test-PRD.xml";
		assertTrue(fsa.getFiles().containsKey(expectedKey));
		String output = fsa.getFiles().get(expectedKey).toString();
		assertTrue(output
				.contains("<bean id=\"Test-A\" class=\"com.temenos.interaction.core.hypermedia.CollectionResourceState\">"));
		assertTrue(output.contains("<constructor-arg name=\"entityName\" value=\"ENTITY\" />"));
		assertTrue(output.contains("<constructor-arg name=\"name\" value=\"A\" />"));
		assertTrue(output.contains("<bean class=\"com.temenos.interaction.core.hypermedia.Action\">"));
		assertTrue(output.contains("<constructor-arg value=\"GetEntity\" />"));
		assertTrue(output.contains("<constructor-arg value=\"VIEW\" />"));
		assertTrue(output.contains("<constructor-arg name=\"path\" value=\"/{companyid}/A\" />"));
	}

	/**
	 * Test generate from incomplete rim.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testGenerateFromIncompleteRIM() throws Exception {
		DomainModel model = parseHelper.parse(INCOMPLETE_RIM);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		boolean exceptionThrown = false;
		try {
			underTest.doGenerate(model.eResource(), fsa);
		} catch (RuntimeException e) {
			exceptionThrown = true;
		}
		assertFalse(exceptionThrown);
	}

	/**
	 * Test generate with null.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testGenerateWithNull() throws Exception {
		boolean exceptionThrown = false;
		try {
			InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
			underTest.doGenerate(null, fsa);
		} catch (RuntimeException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}

	/**
	 * Test generate simple states.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testGenerateSimpleStates() throws Exception {
		DomainModel domainModel = parseHelper.parse(SIMPLE_STATES_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);
		
		Map<String, Object> allFiles = fsa.getAllFiles();

		// Verify keys
		String key = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Simple-PRD.xml";
		assertTrue(allFiles.containsKey(key));
		
		String output = allFiles.get(key).toString();		
		
		String expectedKey1 = "Simple-A";		
		assertTrue(output.contains(expectedKey1));

		String expectedKey2 = "Simple-B";
		assertTrue(output.contains(expectedKey2));

		String expectedKey3 = "Simple-E";
		assertTrue(output.contains(expectedKey3));
	}
	
	@Test
	public void testGenerateDynamicState() throws Exception {
		DomainModel domainModel = parseHelper.parse(DYNAMIC_STATE_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);
		
		Map<String, Object> allFiles = fsa.getAllFiles();

		String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Dynamic-PRD.xml";
		assertTrue(allFiles.containsKey(expectedKey));
		
		String output = allFiles.get(expectedKey).toString();
			
		String resourceA = getResourceBean(output, "Dynamic-A");

		String transitionFindSource = getTransitionBean(resourceA, "findsource");
				
		assertTrue(transitionFindSource.contains("<bean class=\"com.temenos.interaction.core.hypermedia.DynamicResourceState\">"));		
		assertTrue(transitionFindSource.contains("<constructor-arg name=\"entityName\" value=\"ENTITY\" />"));
		assertTrue(transitionFindSource.contains("<constructor-arg name=\"name\" value=\"dynamic\" />"));
		assertTrue(transitionFindSource.contains("<constructor-arg name=\"resourceLocatorName\" value=\"findsource\" />"));
		assertTrue(transitionFindSource.contains("<constructor-arg name=\"resourceLocatorArgs\">"));
		assertTrue(transitionFindSource.contains("<value type=\"java.lang.String[]\">{reference}</value>"));		
		assertTrue(transitionFindSource.contains("<property name=\"label\" value=\"Who &amp; Why\" />"));		
		assertTrue(transitionFindSource.contains("</constructor-arg>"));
		assertTrue(transitionFindSource.contains("</bean>"));		
	}	

	/**
	 * Iterate through properties and assert that one exists for the supplied name and value
	 * @param startElement
	 */
	private void assertProperty(StartElement startElement, String expectedPropertyName, String expectedPropertyValue, String expectedPropertyRef) {
		System.out.println("start property...");
		// attribute
		String actualPropertyName = null;
		String actualPropertyValue = null;
		String actualPropertyRef = null;
		
		Iterator<Attribute> attributes = startElement.getAttributes();
		while (attributes.hasNext()) {
			Attribute attribute = attributes.next();
			System.out.println("attribute.getName() = " + attribute.getName().toString());

			if (actualPropertyName == null && attribute.getName().toString().equals("name")) {
				actualPropertyName = attribute.getValue();
				System.out.println("actualPropertyName = " + actualPropertyName);
			}

			if (actualPropertyValue == null && attribute.getName().toString().equals("value")) {
				actualPropertyValue = attribute.getValue();
				System.out.println("actualPropertyValue = " + actualPropertyValue);
			}

			if (actualPropertyRef == null && attribute.getName().toString().equals("ref")) {
				actualPropertyRef = attribute.getValue();
				System.out.println("actualPropertyRef = " + actualPropertyRef);
			}

		}

		if (expectedPropertyName != null)
			assertEquals(expectedPropertyName, actualPropertyName);
		if (expectedPropertyValue != null)
			assertEquals(expectedPropertyValue, actualPropertyValue);
		if (expectedPropertyRef != null)
			assertEquals(expectedPropertyRef, actualPropertyRef);

		resetState();
	}

	/**
	 * 
	 */
	private void resetState() {
		beanClass = null;
		beanId = null;
	}

	/**
	 * Process resource constructor args.
	 * 
	 * @param startElement
	 *            the start element
	 * @param beanClass
	 *            the bean class
	 */
	private void processActionConstructorArgs(StartElement startElement) {

		// item = new Item();
		System.out.println("constructor...");
		// attribute
		String constructorValue1 = null;
		String constructorValue2 = null;

		Iterator<Attribute> attributes = startElement.getAttributes();
		while (attributes.hasNext()) {
			Attribute attribute = attributes.next();
			System.out.println("attribute.getName() = " + attribute.getName().toString());
			System.out.println("attribute.getValue() = " + attribute.getValue().toString());

			if (constructorValue1 == null) {
				constructorValue1 = attribute.getValue();
				System.out.println("constructorValue1 = " + attribute.getValue());
				assertTrue(constructorValue1.equals("GETServiceDocument") || constructorValue1.equals("VIEW")
						|| constructorValue1.equals("path") || constructorValue1.equals("ENTRY")|| constructorValue1.equals("UpdateEntity") || constructorValue1.equals("GetEntity") || constructorValue1.equals("GETEntities")  || constructorValue1.equals("GetException")  || constructorValue1.equals("GETEntities")  || constructorValue1.equals("GetException")  || constructorValue1.equals("VIEW") || constructorValue1.equals("rels") || constructorValue1.equals("uriSpec") || constructorValue1.equals("errorState") );
			} else if (constructorValue2 == null) {
				constructorValue2 = attribute.getValue();
				System.out.println("constructorValue2 = " + attribute.getValue());
				assertTrue(constructorValue2.equals("/A") || constructorValue2.equals("/")
						|| constructorValue2.equals("/B") || constructorValue2.equals("/E"));

			}

		}

		resetState();
	}

	/**
	 * Process collection resource state constructor args.
	 * 
	 * @param startElement
	 *            the start element
	 * @param beanClass
	 *            the bean class
	 */
	private void processCollectionResourceStateConstructorArgs(StartElement startElement, String beanClass) {

		// item = new Item();
		System.out.println("constructor...");
		// attribute
		String constructorValue1 = null;
		String constructorValue2 = null;

		Iterator<Attribute> attributes = startElement.getAttributes();
		while (attributes.hasNext()) {
			Attribute attribute = attributes.next();
			System.out.println("attribute.getName() = " + attribute.getName().toString());
			System.out.println("attribute.getValue() = " + attribute.getValue().toString());

			if (constructorValue1 == null) {
				constructorValue1 = attribute.getValue();
				System.out.println("constructorValue1 = " + attribute.getValue());
				assertTrue((constructorValue1.equals("entityName")) || (constructorValue1.equals("name")));
			} else if (constructorValue2 == null) {
				constructorValue2 = attribute.getValue();
				System.out.println("constructorValue2 = " + attribute.getValue());
				assertTrue(constructorValue2.equals("ENTITY") || constructorValue2.equals("A")
						|| constructorValue2.equals("B") || constructorValue2.equals("E")
						|| constructorValue2.equals("EXCEPTION"));

			}

		}

		resetState();
	}


	/**
	 * Process resource constructor args.
	 * 
	 * @param startElement
	 *            the start element
	 * @param beanClass
	 *            the bean class
	 */
	private void processResourceStateConstructorArgs(StartElement startElement, String beanClass) {

		// item = new Item();
		System.out.println("constructor...");
		// attribute
		String constructorName = null;
		String constructorValue1 = null;

		Iterator<Attribute> attributes = startElement.getAttributes();
		while (attributes.hasNext()) {
			Attribute attribute = attributes.next();
			System.out.println("attribute.getName() = " + attribute.getName().toString());
			System.out.println("attribute.getValue() = " + attribute.getValue().toString());

			if (constructorName == null) {
				constructorName = attribute.getValue();
				System.out.println("constructorName = " + attribute.getValue());

			} else if (constructorValue1 == null) {
				constructorValue1 = attribute.getValue();
				System.out.println("constructorValue = " + attribute.getValue());
			}

		}

		if ((constructorName != null) && (constructorValue1 != null)) {
			assertTrue(constructorName.equals("entityName") || constructorName.equals("name"));
			assertTrue(constructorValue1.equals("ServiceDocument"));
		}
	}


	/*
	 * General test of a resource with multiple resources 
	 */
	@Test
	public void testGenerateResourceSimplePRD() throws Exception {
		DomainModel domainModel = parseHelper.parse(SIMPLE_STATES_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
		underTest.doGenerate(model.eResource(), fsa);
		
		Map<String, Object> allFiles = fsa.getAllFiles();

		// Verify keys
		String expectedKey2 = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Simple-PRD.xml";
		assertTrue(allFiles.containsKey(expectedKey2));

		String root = (String) allFiles.get(expectedKey2).toString();
		Reader reader = new StringReader(root);


		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = factory.createXMLEventReader(reader);

		boolean simpleAFound = false;
		boolean simpleBFound = false;
		boolean simpleEFound = false;
		
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();

			// reach the start of an item
			if (event.isStartElement()) {

				StartElement startElement = event.asStartElement();
				System.out.println("startElement = " + startElement.getName().getLocalPart());

				if (startElement.getName().getLocalPart() == "bean") {
					System.out.println("start bean...");
					// attribute
					Iterator<Attribute> attributes = startElement.getAttributes();
					while (attributes.hasNext()) {
						Attribute attribute = attributes.next();
						System.out.println("attribute.getName() = " + attribute.getName().toString());

						if (attribute.getName().toString().equals("id")) {
							beanId = attribute.getValue();
							System.out.println("beanId = " + attribute.getValue());
						} else if (attribute.getName().toString().equals("class")) {
							beanClass = attribute.getValue();
							System.out.println("beanClass = " + attribute.getValue());
							if (beanClass.equals(BEAN_ID_COLLECTION_RESOURCE_STATE)) {
								processState = PROCESSING_STATE.COLLECTION_RESOURCE_STATE;
								
								if("Simple-A".equals(beanId)) {
									simpleAFound = true;
								}
								
								if("Simple-E".equals(beanId)) {
									simpleEFound = true;
								}
								
							} else if (beanClass.equals(BEAN_ID_RESOURCE_STATE)) {
								if("Simple-B".equals(beanId)) {
									simpleBFound = true;
								}							
							} else if (beanClass.equals(ACTION)) {
								processState = PROCESSING_STATE.ACTION;
							} else {
								throw new Exception("Invalid beanClass");
							}

						}
					}
					resetState();
					continue;
				}

				if (startElement.getName().getLocalPart() == "property") {
					if (processState == PROCESSING_STATE.FACTORY_BEAN) {
						assertProperty(startElement, "method", "GET", null);
					}
					continue;
				}

				// Process constructor-arg for current beanClass
				if (startElement.getName().getLocalPart() == "constructor-arg") {
					if (processState == PROCESSING_STATE.ACTION) {
						processActionConstructorArgs(startElement);
					} else if (processState == PROCESSING_STATE.RESOURCE_STATE) {
						processResourceStateConstructorArgs(startElement, beanClass);
					} else if (processState == PROCESSING_STATE.COLLECTION_RESOURCE_STATE) {
						processCollectionResourceStateConstructorArgs(startElement, beanClass);
					}

				}

				// data
				if (event.isStartElement()) {
					if (event.asStartElement().getName().getLocalPart().equals("thetext")) {
						event = eventReader.nextEvent();
						System.out.println("thetext: " + event.asCharacters().getData());
					}
				}
			}

			// reach the end of an item
			if (event.isAttribute()) {
				EndElement endElement = event.asEndElement();
				if (endElement.getName().getLocalPart() == "item") {
					System.out.println("--end of an item\n");
				}
			}
			// reach the end of an item
			if (event.isEndElement()) {
				EndElement endElement = event.asEndElement();
				if (endElement.getName().getLocalPart() == "item") {
					System.out.println("--end of an item\n");
				}
			}
			
			// Reset processing state
			processState = PROCESSING_STATE.INIT; 
		}
		
		assertTrue(simpleAFound);
		assertTrue(simpleBFound);
		assertTrue(simpleEFound);		
	}
	private final static String PAGING_RIM = "" + "rim Dynamic {" + LINE_SEP + "    command GetEntity" + LINE_SEP
            + " command UpdateEntity" + LINE_SEP +
            "initial resource A {" + LINE_SEP + "   type: collection" + LINE_SEP + "    entity: ENTITY" + LINE_SEP
            + " view: GetEntity" + LINE_SEP
            + "GET *-> locator findsource(\"{reference}\"){" + LINE_SEP + "parameters [ $top = \"10\" ]" + LINE_SEP +"title: \"Who & Why\"" + LINE_SEP + "}"
            + LINE_SEP + "}" + LINE_SEP
            + "resource B {" + "    type: item" + LINE_SEP + "  entity: ENTITY" + LINE_SEP + "  actions [ UpdateEntity ]"
            + LINE_SEP + "}" + LINE_SEP + "}" + LINE_SEP + "";

	@Test
    public void testGeneratePagingRim() throws Exception {
        DomainModel domainModel = parseHelper.parse(PAGING_RIM);
        ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
        InMemoryFileSystemAccess fsa = new InMemoryFileSystemAccess();
        underTest.doGenerate(model.eResource(), fsa);
        
        Map<String, Object> allFiles = fsa.getAllFiles();

        String expectedKey = IFileSystemAccess.DEFAULT_OUTPUT + "IRIS-Paging-PRD.xml";
        assertTrue(fsa.getFiles().containsKey(expectedKey));
        String output = fsa.getFiles().get(expectedKey).toString();
        System.out.println("IRIS-Paging-PRD.xml File output : "+output);
        //System.error.println(output);        
        output = removeFormatting(output);

        assertTrue(output.contains("<property name=\"uriParameters\"><util:map>"));
        assertTrue(output.contains("<entry key=\"$top\" value=\"10\"/>"));
    }
	
}
