package com.ldbc.driver.workloads.ldbc.snb.interactive;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.ldbc.driver.*;
import com.ldbc.driver.control.ConsoleAndFileDriverConfiguration;
import com.ldbc.driver.csv.charseeker.*;
import com.ldbc.driver.generator.CsvEventStreamReaderBasicCharSeeker;
import com.ldbc.driver.generator.GeneratorFactory;
import com.ldbc.driver.generator.RandomDataGeneratorFactory;
import com.ldbc.driver.util.ClassLoaderHelper;
import com.ldbc.driver.util.ClassLoadingException;
import com.ldbc.driver.util.Tuple;
import com.ldbc.driver.csv.simple.SimpleCsvFileReader;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LdbcSnbInteractiveWorkload extends Workload {
    private List<Closeable> forumUpdateOperationsFileReaders = new ArrayList<>();
    private List<File> forumUpdateOperationFiles = new ArrayList<>();
    private List<Closeable> personUpdateOperationsFileReaders = new ArrayList<>();
    private List<File> personUpdateOperationFiles = new ArrayList<>();

    private List<Closeable> readOperationFileReaders = new ArrayList<>();
    private File readOperation1File;
    private File readOperation2File;
    private File readOperation3File;
    private File readOperation4File;
    private File readOperation5File;
    private File readOperation6File;
    private File readOperation7File;
    private File readOperation8File;
    private File readOperation9File;
    private File readOperation10File;
    private File readOperation11File;
    private File readOperation12File;
    private File readOperation13File;
    private File readOperation14File;

    private long readOperation1InterleaveAsMilli;
    private long readOperation2InterleaveAsMilli;
    private long readOperation3InterleaveAsMilli;
    private long readOperation4InterleaveAsMilli;
    private long readOperation5InterleaveAsMilli;
    private long readOperation6InterleaveAsMilli;
    private long readOperation7InterleaveAsMilli;
    private long readOperation8InterleaveAsMilli;
    private long readOperation9InterleaveAsMilli;
    private long readOperation10InterleaveAsMilli;
    private long readOperation11InterleaveAsMilli;
    private long readOperation12InterleaveAsMilli;
    private long readOperation13InterleaveAsMilli;
    private long readOperation14InterleaveAsMilli;

    private long updateInterleaveAsMilli;
    private double compressionRatio;
    private double shortReadDissipationFactor;

    private Set<Class> enabledLongReadOperationTypes;
    private Set<Class> enabledShortReadOperationTypes;
    private Set<Class> enabledWriteOperationTypes;
    private LdbcSnbInteractiveConfiguration.UpdateStreamParser parser;

    @Override
    public Map<Integer, Class<? extends Operation>> operationTypeToClassMapping(Map<String, String> params) {
        return LdbcSnbInteractiveConfiguration.operationTypeToClassMapping();
    }

    @Override
    public void onInit(Map<String, String> params) throws WorkloadException {
        List<String> compulsoryKeys = Lists.newArrayList(
                LdbcSnbInteractiveConfiguration.PARAMETERS_DIRECTORY);

        compulsoryKeys.addAll(LdbcSnbInteractiveConfiguration.LONG_READ_OPERATION_ENABLE_KEYS);
        compulsoryKeys.addAll(LdbcSnbInteractiveConfiguration.WRITE_OPERATION_ENABLE_KEYS);

        Set<String> missingPropertyParameters = LdbcSnbInteractiveConfiguration.missingParameters(params, compulsoryKeys);
        if (false == missingPropertyParameters.isEmpty())
            throw new WorkloadException(String.format("Workload could not initialize due to missing parameters: %s", missingPropertyParameters.toString()));

        if (params.containsKey(LdbcSnbInteractiveConfiguration.UPDATES_DIRECTORY)) {
            String updatesDirectoryPath = params.get(LdbcSnbInteractiveConfiguration.UPDATES_DIRECTORY);
            File updatesDirectory = new File(updatesDirectoryPath);
            if (false == updatesDirectory.exists())
                throw new WorkloadException(String.format("Updates directory does not exist\nDirectory: %s", updatesDirectory.getAbsolutePath()));
            if (false == updatesDirectory.isDirectory())
                throw new WorkloadException(String.format("Updates directory is not a directory\nDirectory: %s", updatesDirectory.getAbsolutePath()));
            forumUpdateOperationFiles = LdbcSnbInteractiveConfiguration.forumUpdateFilesInDirectory(updatesDirectory);
            personUpdateOperationFiles = LdbcSnbInteractiveConfiguration.personUpdateFilesInDirectory(updatesDirectory);
        } else {
            forumUpdateOperationFiles = new ArrayList<>();
            personUpdateOperationFiles = new ArrayList<>();
        }

        File parametersDir = new File(params.get(LdbcSnbInteractiveConfiguration.PARAMETERS_DIRECTORY));
        if (false == parametersDir.exists()) {
            throw new WorkloadException(String.format("Parameters directory does not exist: %s", parametersDir.getAbsolutePath()));
        }
        for (String readOperationParamsFilename : LdbcSnbInteractiveConfiguration.READ_OPERATION_PARAMS_FILENAMES) {
            String readOperationParamsFullPath = parametersDir.getAbsolutePath() + "/" + readOperationParamsFilename;
            if (false == new File(readOperationParamsFullPath).exists()) {
                throw new WorkloadException(String.format("Read operation parameters file does not exist: %s", readOperationParamsFullPath));
            }
        }
        readOperation1File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_1_PARAMS_FILENAME);
        readOperation2File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_2_PARAMS_FILENAME);
        readOperation3File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_3_PARAMS_FILENAME);
        readOperation4File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_4_PARAMS_FILENAME);
        readOperation5File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_5_PARAMS_FILENAME);
        readOperation7File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_7_PARAMS_FILENAME);
        readOperation8File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_8_PARAMS_FILENAME);
        readOperation9File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_9_PARAMS_FILENAME);
        readOperation6File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_6_PARAMS_FILENAME);
        readOperation10File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_10_PARAMS_FILENAME);
        readOperation11File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_11_PARAMS_FILENAME);
        readOperation12File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_12_PARAMS_FILENAME);
        readOperation13File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_13_PARAMS_FILENAME);
        readOperation14File = new File(parametersDir, LdbcSnbInteractiveConfiguration.READ_OPERATION_14_PARAMS_FILENAME);

        enabledLongReadOperationTypes = new HashSet<>();
        for (String longReadOperationEnableKey : LdbcSnbInteractiveConfiguration.LONG_READ_OPERATION_ENABLE_KEYS) {
            String longReadOperationEnabledString = params.get(longReadOperationEnableKey);
            Boolean longReadOperationEnabled = Boolean.parseBoolean(longReadOperationEnabledString);
            String longReadOperationClassName = LdbcSnbInteractiveConfiguration.LDBC_INTERACTIVE_PACKAGE_PREFIX +
                    LdbcSnbInteractiveConfiguration.removePrefix(
                            LdbcSnbInteractiveConfiguration.removeSuffix(
                                    longReadOperationEnableKey,
                                    LdbcSnbInteractiveConfiguration.ENABLE_SUFFIX
                            ),
                            LdbcSnbInteractiveConfiguration.LDBC_SNB_INTERACTIVE_PARAM_NAME_PREFIX
                    );
            try {
                Class longReadOperationClass = ClassLoaderHelper.loadClass(longReadOperationClassName);
                if (longReadOperationEnabled) enabledLongReadOperationTypes.add(longReadOperationClass);
            } catch (ClassLoadingException e) {
                throw new WorkloadException(
                        String.format("Unable to load operation class for parameter: %s\nGuessed incorrect class name: %s",
                                longReadOperationEnableKey, longReadOperationClassName),
                        e
                );
            }
        }

        enabledShortReadOperationTypes = new HashSet<>();
        for (String shortReadOperationEnableKey : LdbcSnbInteractiveConfiguration.SHORT_READ_OPERATION_ENABLE_KEYS) {
            String shortReadOperationEnabledString = params.get(shortReadOperationEnableKey);
            Boolean shortReadOperationEnabled = Boolean.parseBoolean(shortReadOperationEnabledString);
            String shortReadOperationClassName = LdbcSnbInteractiveConfiguration.LDBC_INTERACTIVE_PACKAGE_PREFIX +
                    LdbcSnbInteractiveConfiguration.removePrefix(
                            LdbcSnbInteractiveConfiguration.removeSuffix(
                                    shortReadOperationEnableKey,
                                    LdbcSnbInteractiveConfiguration.ENABLE_SUFFIX
                            ),
                            LdbcSnbInteractiveConfiguration.LDBC_SNB_INTERACTIVE_PARAM_NAME_PREFIX
                    );
            try {
                Class shortReadOperationClass = ClassLoaderHelper.loadClass(shortReadOperationClassName);
                if (shortReadOperationEnabled) enabledShortReadOperationTypes.add(shortReadOperationClass);
            } catch (ClassLoadingException e) {
                throw new WorkloadException(
                        String.format("Unable to load operation class for parameter: %s\nGuessed incorrect class name: %s",
                                shortReadOperationEnableKey, shortReadOperationClassName),
                        e
                );
            }
        }
        if (false == enabledShortReadOperationTypes.isEmpty()) {
            if (false == params.containsKey(LdbcSnbInteractiveConfiguration.SHORT_READ_DISSIPATION)) {
                throw new WorkloadException(String.format("Configuration parameter missing: %s", LdbcSnbInteractiveConfiguration.SHORT_READ_DISSIPATION));
            }
            shortReadDissipationFactor = Double.parseDouble(params.get(LdbcSnbInteractiveConfiguration.SHORT_READ_DISSIPATION));
            if (shortReadDissipationFactor < 0 || shortReadDissipationFactor > 1) {
                throw new WorkloadException(String.format("Configuration parameter %s should be in interval [1.0,0.0] but is: %s", shortReadDissipationFactor));
            }
        }

        enabledWriteOperationTypes = new HashSet<>();
        for (String writeOperationEnableKey : LdbcSnbInteractiveConfiguration.WRITE_OPERATION_ENABLE_KEYS) {
            String writeOperationEnabledString = params.get(writeOperationEnableKey);
            Boolean writeOperationEnabled = Boolean.parseBoolean(writeOperationEnabledString);
            String writeOperationClassName = LdbcSnbInteractiveConfiguration.LDBC_INTERACTIVE_PACKAGE_PREFIX +
                    LdbcSnbInteractiveConfiguration.removePrefix(
                            LdbcSnbInteractiveConfiguration.removeSuffix(
                                    writeOperationEnableKey,
                                    LdbcSnbInteractiveConfiguration.ENABLE_SUFFIX
                            ),
                            LdbcSnbInteractiveConfiguration.LDBC_SNB_INTERACTIVE_PARAM_NAME_PREFIX
                    );
            try {
                Class writeOperationClass = ClassLoaderHelper.loadClass(writeOperationClassName);
                if (writeOperationEnabled) enabledWriteOperationTypes.add(writeOperationClass);
            } catch (ClassLoadingException e) {
                throw new WorkloadException(
                        String.format("Unable to load operation class for parameter: %s\nGuessed incorrect class name: %s",
                                writeOperationEnableKey, writeOperationClassName),
                        e
                );
            }
        }

        List<String> frequencyKeys = Lists.newArrayList(LdbcSnbInteractiveConfiguration.READ_OPERATION_FREQUENCY_KEYS);
        Set<String> missingFrequencyKeys = LdbcSnbInteractiveConfiguration.missingParameters(params, frequencyKeys);
        if (enabledWriteOperationTypes.isEmpty() && false == params.containsKey(LdbcSnbInteractiveConfiguration.UPDATE_INTERLEAVE)) {
            // if UPDATE_INTERLEAVE is missing, set it to DEFAULT
            params.put(LdbcSnbInteractiveConfiguration.UPDATE_INTERLEAVE, LdbcSnbInteractiveConfiguration.DEFAULT_UPDATE_INTERLEAVE);
        }
        if (false == params.containsKey(LdbcSnbInteractiveConfiguration.UPDATE_INTERLEAVE)) {
            throw new WorkloadException(String.format("Workload could not initialize. Missing parameter: %s", LdbcSnbInteractiveConfiguration.UPDATE_INTERLEAVE));
        }
        updateInterleaveAsMilli = Integer.parseInt(params.get(LdbcSnbInteractiveConfiguration.UPDATE_INTERLEAVE));
        if (missingFrequencyKeys.isEmpty()) {
            // compute interleave based on frequencies
            params = LdbcSnbInteractiveConfiguration.convertFrequenciesToInterleaves(params);
        } else {
            // if any frequencies are not set, there should be specified interleave times for read queries
            List<String> interleaveKeys = Lists.newArrayList(LdbcSnbInteractiveConfiguration.READ_OPERATION_INTERLEAVE_KEYS);
            Set<String> missingInterleaveKeys = LdbcSnbInteractiveConfiguration.missingParameters(params, interleaveKeys);
            if (false == missingInterleaveKeys.isEmpty()) {
                throw new WorkloadException(String.format("Workload could not initialize. One of the following groups of parameters should be set: %s or %s", missingFrequencyKeys.toString(), missingInterleaveKeys.toString()));
            }
        }

        try {
            readOperation1InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_1_INTERLEAVE_KEY));
            readOperation2InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_2_INTERLEAVE_KEY));
            readOperation3InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_3_INTERLEAVE_KEY));
            readOperation4InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_4_INTERLEAVE_KEY));
            readOperation5InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_5_INTERLEAVE_KEY));
            readOperation6InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_6_INTERLEAVE_KEY));
            readOperation7InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_7_INTERLEAVE_KEY));
            readOperation8InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_8_INTERLEAVE_KEY));
            readOperation9InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_9_INTERLEAVE_KEY));
            readOperation10InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_10_INTERLEAVE_KEY));
            readOperation11InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_11_INTERLEAVE_KEY));
            readOperation12InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_12_INTERLEAVE_KEY));
            readOperation13InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_13_INTERLEAVE_KEY));
            readOperation14InterleaveAsMilli = Long.parseLong(params.get(LdbcSnbInteractiveConfiguration.READ_OPERATION_14_INTERLEAVE_KEY));
        } catch (NumberFormatException e) {
            throw new WorkloadException("Unable to parse one of the read operation interleave values", e);
        }

        String parserString = params.get(LdbcSnbInteractiveConfiguration.UPDATE_STREAM_PARSER);
        if (null == parserString)
            parserString = LdbcSnbInteractiveConfiguration.DEFAULT_UPDATE_STREAM_PARSER.name();
        if (false == LdbcSnbInteractiveConfiguration.isValidParser(parserString)) {
            throw new WorkloadException("Invalid parser: " + parserString);
        }
        this.parser = LdbcSnbInteractiveConfiguration.UpdateStreamParser.valueOf(parserString);
        this.compressionRatio = Double.parseDouble(params.get(ConsoleAndFileDriverConfiguration.TIME_COMPRESSION_RATIO_ARG));
    }

    @Override
    synchronized protected void onClose() throws IOException {
        for (Closeable forumUpdateOperationsFileReader : forumUpdateOperationsFileReaders) {
            forumUpdateOperationsFileReader.close();
        }

        for (Closeable personUpdateOperationsFileReader : personUpdateOperationsFileReaders) {
            personUpdateOperationsFileReader.close();
        }

        for (Closeable readOperationFileReader : readOperationFileReaders) {
            readOperationFileReader.close();
        }
    }

    private Tuple.Tuple2<Iterator<Operation>, Closeable> fileToWriteStreamParser(File updateOperationsFile, LdbcSnbInteractiveConfiguration.UpdateStreamParser parser) throws IOException, WorkloadException {
        switch (parser) {
            case REGEX: {
                SimpleCsvFileReader csvFileReader = new SimpleCsvFileReader(updateOperationsFile, SimpleCsvFileReader.DEFAULT_COLUMN_SEPARATOR_REGEX_STRING);
                return Tuple.<Iterator<Operation>, Closeable>tuple2(WriteEventStreamReaderRegex.create(csvFileReader), csvFileReader);
            }
            case CHAR_SEEKER: {
                int bufferSize = 1 * 1024 * 1024;
//                BufferedCharSeeker charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(updateOperationsFile)), bufferSize);
                BufferedCharSeeker charSeeker = new BufferedCharSeeker(
                        Readables.wrap(
                                new InputStreamReader(new FileInputStream(updateOperationsFile), Charsets.UTF_8)
                        ),
                        bufferSize
                );
                Extractors extractors = new Extractors(';', ',');
                return Tuple.<Iterator<Operation>, Closeable>tuple2(WriteEventStreamReaderCharSeeker.create(charSeeker, extractors, '|'), charSeeker);
            }
            case CHAR_SEEKER_THREAD: {
                int bufferSize = 1 * 1024 * 1024;
                BufferedCharSeeker charSeeker = new BufferedCharSeeker(
                        ThreadAheadReadable.threadAhead(
                                Readables.wrap(
                                        new InputStreamReader(new FileInputStream(updateOperationsFile), Charsets.UTF_8)
                                ),
                                bufferSize
                        ),
                        bufferSize
                );
                Extractors extractors = new Extractors(';', ',');
                return Tuple.<Iterator<Operation>, Closeable>tuple2(WriteEventStreamReaderCharSeeker.create(charSeeker, extractors, '|'), charSeeker);
            }
        }
        SimpleCsvFileReader csvFileReader = new SimpleCsvFileReader(updateOperationsFile, SimpleCsvFileReader.DEFAULT_COLUMN_SEPARATOR_REGEX_STRING);
        return Tuple.<Iterator<Operation>, Closeable>tuple2(WriteEventStreamReaderRegex.create(csvFileReader), csvFileReader);
    }

    @Override
    protected WorkloadStreams getStreams(GeneratorFactory gf, boolean hasDbConnected) throws WorkloadException {
        long workloadStartTimeAsMilli = Long.MAX_VALUE;
        WorkloadStreams ldbcSnbInteractiveWorkloadStreams = new WorkloadStreams();
        List<Iterator<?>> asynchronousDependencyStreamsList = new ArrayList<>();
        List<Iterator<?>> asynchronousNonDependencyStreamsList = new ArrayList<>();
        Set<Class<? extends Operation>> dependentAsynchronousOperationTypes = Sets.newHashSet();
        Set<Class<? extends Operation>> dependencyAsynchronousOperationTypes = Sets.newHashSet();

        /* *******
         * *******
         * *******
         *  WRITES
         * *******
         * *******
         * *******/

         /*
         * Create person write operation streams
         */
        if (enabledWriteOperationTypes.contains(LdbcUpdate1AddPerson.class)) {
            for (File personUpdateOperationFile : personUpdateOperationFiles) {
                Iterator<Operation> personUpdateOperationsParser;
                try {
                    Tuple.Tuple2<Iterator<Operation>, Closeable> parserAndCloseable = fileToWriteStreamParser(personUpdateOperationFile, parser);
                    personUpdateOperationsParser = parserAndCloseable._1();
                    personUpdateOperationsFileReaders.add(parserAndCloseable._2());
                } catch (IOException e) {
                    throw new WorkloadException("Unable to open person update stream: " + personUpdateOperationFile.getAbsolutePath(), e);
                }
                if (false == personUpdateOperationsParser.hasNext()) {
                    // Update stream is empty
                    System.out.println(
                            String.format(""
                                            + "***********************************************\n"
                                            + "  !! WARMING !!\n"
                                            + "  Update stream is empty: %s\n"
                                            + "  Check that data generation process completed successfully\n"
                                            + "***********************************************",
                                    personUpdateOperationFile.getAbsolutePath()
                            )
                    );
                    continue;
                }
                PeekingIterator<Operation> unfilteredPersonUpdateOperations = Iterators.peekingIterator(personUpdateOperationsParser);

                try {
                    if (unfilteredPersonUpdateOperations.peek().scheduledStartTimeAsMilli() < workloadStartTimeAsMilli) {
                        workloadStartTimeAsMilli = unfilteredPersonUpdateOperations.peek().scheduledStartTimeAsMilli();
                    }
                } catch (NoSuchElementException e) {
                    // do nothing, exception just means that stream was empty
                }

                // Filter Write Operations
                Predicate<Operation> enabledWriteOperationsFilter = new Predicate<Operation>() {
                    @Override
                    public boolean apply(Operation operation) {
                        return enabledWriteOperationTypes.contains(operation.getClass());
                    }
                };
                Iterator<Operation> filteredPersonUpdateOperations = Iterators.filter(unfilteredPersonUpdateOperations, enabledWriteOperationsFilter);

                Set<Class<? extends Operation>> dependentPersonUpdateOperationTypes = Sets.newHashSet();
                Set<Class<? extends Operation>> dependencyPersonUpdateOperationTypes = Sets.<Class<? extends Operation>>newHashSet(
                        LdbcUpdate1AddPerson.class
                );

                ChildOperationGenerator personUpdateChildOperationGenerator = null;

                ldbcSnbInteractiveWorkloadStreams.addBlockingStream(
                        dependentPersonUpdateOperationTypes,
                        dependencyPersonUpdateOperationTypes,
                        filteredPersonUpdateOperations,
                        Collections.<Operation>emptyIterator(),
                        personUpdateChildOperationGenerator
                );
            }
        }

        /*
         * Create forum write operation streams
         */
        if (enabledWriteOperationTypes.contains(LdbcUpdate2AddPostLike.class) ||
                enabledWriteOperationTypes.contains(LdbcUpdate3AddCommentLike.class) ||
                enabledWriteOperationTypes.contains(LdbcUpdate4AddForum.class) ||
                enabledWriteOperationTypes.contains(LdbcUpdate5AddForumMembership.class) ||
                enabledWriteOperationTypes.contains(LdbcUpdate6AddPost.class) ||
                enabledWriteOperationTypes.contains(LdbcUpdate7AddComment.class) ||
                enabledWriteOperationTypes.contains(LdbcUpdate8AddFriendship.class)
                ) {
            for (File forumUpdateOperationFile : forumUpdateOperationFiles) {
                Iterator<Operation> forumUpdateOperationsParser;
                try {
                    Tuple.Tuple2<Iterator<Operation>, Closeable> parserAndCloseable = fileToWriteStreamParser(forumUpdateOperationFile, parser);
                    forumUpdateOperationsParser = parserAndCloseable._1();
                    forumUpdateOperationsFileReaders.add(parserAndCloseable._2());
                } catch (IOException e) {
                    throw new WorkloadException("Unable to open forum update stream: " + forumUpdateOperationFile.getAbsolutePath(), e);
                }
                if (false == forumUpdateOperationsParser.hasNext()) {
                    // Update stream is empty
                    System.out.println(
                            String.format(""
                                            + "***********************************************\n"
                                            + "  !! WARMING !!\n"
                                            + "  Update stream is empty: %s\n"
                                            + "  Check that data generation process completed successfully\n"
                                            + "***********************************************",
                                    forumUpdateOperationFile.getAbsolutePath()
                            )
                    );
                    continue;
                }
                PeekingIterator<Operation> unfilteredForumUpdateOperations = Iterators.peekingIterator(forumUpdateOperationsParser);

                try {
                    if (unfilteredForumUpdateOperations.peek().scheduledStartTimeAsMilli() < workloadStartTimeAsMilli) {
                        workloadStartTimeAsMilli = unfilteredForumUpdateOperations.peek().scheduledStartTimeAsMilli();
                    }
                } catch (NoSuchElementException e) {
                    // do nothing, exception just means that stream was empty
                }

                // Filter Write Operations
                Predicate<Operation> enabledWriteOperationsFilter = new Predicate<Operation>() {
                    @Override
                    public boolean apply(Operation operation) {
                        return enabledWriteOperationTypes.contains(operation.getClass());
                    }
                };
                Iterator<Operation> filteredForumUpdateOperations = Iterators.filter(unfilteredForumUpdateOperations, enabledWriteOperationsFilter);

                Set<Class<? extends Operation>> dependentForumUpdateOperationTypes = Sets.<Class<? extends Operation>>newHashSet(
                        LdbcUpdate2AddPostLike.class,
                        LdbcUpdate3AddCommentLike.class,
                        LdbcUpdate4AddForum.class,
                        LdbcUpdate5AddForumMembership.class,
                        LdbcUpdate6AddPost.class,
                        LdbcUpdate7AddComment.class,
                        LdbcUpdate8AddFriendship.class
                );
                Set<Class<? extends Operation>> dependencyForumUpdateOperationTypes = Sets.newHashSet();

                ChildOperationGenerator forumUpdateChildOperationGenerator = null;

                ldbcSnbInteractiveWorkloadStreams.addBlockingStream(
                        dependentForumUpdateOperationTypes,
                        dependencyForumUpdateOperationTypes,
                        Collections.<Operation>emptyIterator(),
                        filteredForumUpdateOperations,
                        forumUpdateChildOperationGenerator
                );
            }
        }

        if (Long.MAX_VALUE == workloadStartTimeAsMilli) workloadStartTimeAsMilli = 0;

        /* *******
         * *******
         * *******
         *  LONG READS
         * *******
         * *******
         * *******/

        /*
         * Create read operation streams, with specified interleaves
         */
        int bufferSize = 1 * 1024 * 1024;
        char columnDelimiter = '|';
        char arrayDelimiter = ';';
        char tupleDelimiter = ',';

        Iterator<Operation> readOperation1Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query1EventStreamReader.Query1Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation1File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation1File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation1File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation1StreamWithoutTimes = new Query1EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation1StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation1InterleaveAsMilli, readOperation1InterleaveAsMilli);

            readOperation1Stream = gf.assignStartTimes(
                    operation1StartTimes,
                    operation1StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation2Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query2EventStreamReader.Query2Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation2File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation2File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation2File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation2StreamWithoutTimes = new Query2EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation2StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation2InterleaveAsMilli, readOperation2InterleaveAsMilli);

            readOperation2Stream = gf.assignStartTimes(
                    operation2StartTimes,
                    operation2StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation3Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query3EventStreamReader.Query3Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation3File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation3File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation3File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation3StreamWithoutTimes = new Query3EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation3StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation3InterleaveAsMilli, readOperation3InterleaveAsMilli);

            readOperation3Stream = gf.assignStartTimes(
                    operation3StartTimes,
                    operation3StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation4Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query4EventStreamReader.Query4Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation4File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation4File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation4File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation4StreamWithoutTimes = new Query4EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation4StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation4InterleaveAsMilli, readOperation4InterleaveAsMilli);

            readOperation4Stream = gf.assignStartTimes(
                    operation4StartTimes,
                    operation4StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation5Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query5EventStreamReader.Query5Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation5File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation5File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation5File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation5StreamWithoutTimes = new Query5EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation5StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation5InterleaveAsMilli, readOperation5InterleaveAsMilli);

            readOperation5Stream = gf.assignStartTimes(
                    operation5StartTimes,
                    operation5StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation6Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query6EventStreamReader.Query6Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation6File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation6File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation6File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation6StreamWithoutTimes = new Query6EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation6StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation6InterleaveAsMilli, readOperation6InterleaveAsMilli);

            readOperation6Stream = gf.assignStartTimes(
                    operation6StartTimes,
                    operation6StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation7Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query7EventStreamReader.Query7Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation7File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation7File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation7File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation7StreamWithoutTimes = new Query7EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation7StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation7InterleaveAsMilli, readOperation7InterleaveAsMilli);

            readOperation7Stream = gf.assignStartTimes(
                    operation7StartTimes,
                    operation7StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation8Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query8EventStreamReader.Query8Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation8File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation8File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation8File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation8StreamWithoutTimes = new Query8EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation8StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation8InterleaveAsMilli, readOperation8InterleaveAsMilli);

            readOperation8Stream = gf.assignStartTimes(
                    operation8StartTimes,
                    operation8StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation9Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query9EventStreamReader.Query9Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation9File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation9File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation9File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation9StreamWithoutTimes = new Query9EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation9StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation9InterleaveAsMilli, readOperation9InterleaveAsMilli);

            readOperation9Stream = gf.assignStartTimes(
                    operation9StartTimes,
                    operation9StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation10Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query10EventStreamReader.Query10Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation10File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation10File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation10File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation10StreamWithoutTimes = new Query10EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation10StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation10InterleaveAsMilli, readOperation10InterleaveAsMilli);

            readOperation10Stream = gf.assignStartTimes(
                    operation10StartTimes,
                    operation10StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation11Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query11EventStreamReader.Query11Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation11File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation11File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation11File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation11StreamWithoutTimes = new Query11EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation11StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation11InterleaveAsMilli, readOperation11InterleaveAsMilli);

            readOperation11Stream = gf.assignStartTimes(
                    operation11StartTimes,
                    operation11StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation12Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query12EventStreamReader.Query12Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation12File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation12File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation12File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation12StreamWithoutTimes = new Query12EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation12StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation12InterleaveAsMilli, readOperation12InterleaveAsMilli);

            readOperation12Stream = gf.assignStartTimes(
                    operation12StartTimes,
                    operation12StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation13Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query13EventStreamReader.Query13Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation13File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation13File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation13File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation13StreamWithoutTimes = new Query13EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation13StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation13InterleaveAsMilli, readOperation13InterleaveAsMilli);

            readOperation13Stream = gf.assignStartTimes(
                    operation13StartTimes,
                    operation13StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        Iterator<Operation> readOperation14Stream;
        {
            CsvEventStreamReaderBasicCharSeeker.EventDecoder<Object[]> decoder = new Query14EventStreamReader.Query14Decoder();
            Extractors extractors = new Extractors(arrayDelimiter, tupleDelimiter);
            CharSeeker charSeeker;
            try {
                charSeeker = new BufferedCharSeeker(Readables.wrap(new FileReader(readOperation14File)), bufferSize);
            } catch (FileNotFoundException e) {
                throw new WorkloadException(String.format("Unable to open parameters file: %s", readOperation14File.getAbsolutePath()), e);
            }
            Mark mark = new Mark();
            // skip headers
            try {
                charSeeker.seek(mark, new int[]{columnDelimiter});
                charSeeker.seek(mark, new int[]{columnDelimiter});
            } catch (IOException e) {
                throw new WorkloadException(String.format("Unable to advance parameters file beyond headers: %s", readOperation14File.getAbsolutePath()), e);
            }

            Iterator<Operation> operation14StreamWithoutTimes = new Query14EventStreamReader(
                    gf.repeating(
                            new CsvEventStreamReaderBasicCharSeeker<>(
                                    charSeeker,
                                    extractors,
                                    mark,
                                    decoder,
                                    columnDelimiter
                            )
                    )
            );

            Iterator<Long> operation14StartTimes = gf.incrementing(workloadStartTimeAsMilli + readOperation14InterleaveAsMilli, readOperation14InterleaveAsMilli);

            readOperation14Stream = gf.assignStartTimes(
                    operation14StartTimes,
                    operation14StreamWithoutTimes
            );

            readOperationFileReaders.add(charSeeker);
        }

        if (enabledLongReadOperationTypes.contains(LdbcQuery1.class))
            asynchronousNonDependencyStreamsList.add(readOperation1Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery2.class))
            asynchronousNonDependencyStreamsList.add(readOperation2Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery3.class))
            asynchronousNonDependencyStreamsList.add(readOperation3Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery4.class))
            asynchronousNonDependencyStreamsList.add(readOperation4Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery5.class))
            asynchronousNonDependencyStreamsList.add(readOperation5Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery6.class))
            asynchronousNonDependencyStreamsList.add(readOperation6Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery7.class))
            asynchronousNonDependencyStreamsList.add(readOperation7Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery8.class))
            asynchronousNonDependencyStreamsList.add(readOperation8Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery9.class))
            asynchronousNonDependencyStreamsList.add(readOperation9Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery10.class))
            asynchronousNonDependencyStreamsList.add(readOperation10Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery11.class))
            asynchronousNonDependencyStreamsList.add(readOperation11Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery12.class))
            asynchronousNonDependencyStreamsList.add(readOperation12Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery13.class))
            asynchronousNonDependencyStreamsList.add(readOperation13Stream);
        if (enabledLongReadOperationTypes.contains(LdbcQuery14.class))
            asynchronousNonDependencyStreamsList.add(readOperation14Stream);

        /*
         * Merge all dependency asynchronous operation streams, ordered by operation start times
         */
        Iterator<Operation> asynchronousDependencyStreams = gf.mergeSortOperationsByTimeStamp(
                asynchronousDependencyStreamsList.toArray(new Iterator[asynchronousDependencyStreamsList.size()])
        );
        /*
         * Merge all non dependency asynchronous operation streams, ordered by operation start times
         */
        Iterator<Operation> asynchronousNonDependencyStreams = gf.mergeSortOperationsByTimeStamp(
                asynchronousNonDependencyStreamsList.toArray(new Iterator[asynchronousNonDependencyStreamsList.size()])
        );

        /* *******
         * *******
         * *******
         *  SHORT READS
         * *******
         * *******
         * *******/

        ChildOperationGenerator shortReadsChildGenerator = null;
        if (false == enabledShortReadOperationTypes.isEmpty()) {
            Map<Integer, Long> longReadInterleavesAsMilli = new HashMap<>();
            longReadInterleavesAsMilli.put(LdbcQuery1.TYPE, readOperation1InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery2.TYPE, readOperation2InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery3.TYPE, readOperation3InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery4.TYPE, readOperation4InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery5.TYPE, readOperation5InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery6.TYPE, readOperation6InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery7.TYPE, readOperation7InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery8.TYPE, readOperation8InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery9.TYPE, readOperation9InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery10.TYPE, readOperation10InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery11.TYPE, readOperation11InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery12.TYPE, readOperation12InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery13.TYPE, readOperation13InterleaveAsMilli);
            longReadInterleavesAsMilli.put(LdbcQuery14.TYPE, readOperation14InterleaveAsMilli);

            RandomDataGeneratorFactory randomFactory = new RandomDataGeneratorFactory(42l);
            double initialProbability = 1.0;
            Queue<Long> personIdBuffer = (hasDbConnected)
                    ? LdbcSnbShortReadGenerator.synchronizedCircularQueueBuffer(1024)
                    : LdbcSnbShortReadGenerator.constantBuffer(1);
            Queue<Long> messageIdBuffer = (hasDbConnected)
                    ? LdbcSnbShortReadGenerator.synchronizedCircularQueueBuffer(1024)
                    : LdbcSnbShortReadGenerator.constantBuffer(1);
            LdbcSnbShortReadGenerator.SCHEDULED_START_TIME_POLICY scheduledStartTimePolicy = (hasDbConnected)
                    ? LdbcSnbShortReadGenerator.SCHEDULED_START_TIME_POLICY.PREVIOUS_OPERATION_ACTUAL_FINISH_TIME
                    : LdbcSnbShortReadGenerator.SCHEDULED_START_TIME_POLICY.PREVIOUS_OPERATION_SCHEDULED_START_TIME;
            LdbcSnbShortReadGenerator.BufferReplenishFun bufferReplenishFun = (hasDbConnected)
                    ? new LdbcSnbShortReadGenerator.ResultBufferReplenishFun(personIdBuffer, messageIdBuffer)
                    : new LdbcSnbShortReadGenerator.NoOpBufferReplenishFun();
            shortReadsChildGenerator = new LdbcSnbShortReadGenerator(
                    initialProbability,
                    shortReadDissipationFactor,
                    updateInterleaveAsMilli,
                    enabledShortReadOperationTypes,
                    compressionRatio,
                    personIdBuffer,
                    messageIdBuffer,
                    randomFactory,
                    longReadInterleavesAsMilli,
                    scheduledStartTimePolicy,
                    bufferReplenishFun
            );
        }

        /* **************
         * **************
         * **************
         *  FINAL STREAMS
         * **************
         * **************
         * **************/

        ldbcSnbInteractiveWorkloadStreams.setAsynchronousStream(
                dependentAsynchronousOperationTypes,
                dependencyAsynchronousOperationTypes,
                asynchronousDependencyStreams,
                asynchronousNonDependencyStreams,
                shortReadsChildGenerator
        );

        return ldbcSnbInteractiveWorkloadStreams;
    }

    @Override
    public DbValidationParametersFilter dbValidationParametersFilter(Integer requiredValidationParameterCount) {
        final Set<Class> multiResultOperations = Sets.<Class>newHashSet(
                LdbcShortQuery2PersonPosts.class,
                LdbcShortQuery3PersonFriends.class,
                LdbcShortQuery7MessageReplies.class,
                LdbcQuery1.class,
                LdbcQuery2.class,
//                LdbcQuery3.class,
                LdbcQuery4.class,
                LdbcQuery5.class,
                LdbcQuery6.class,
                LdbcQuery7.class,
                LdbcQuery8.class,
                LdbcQuery9.class,
                LdbcQuery10.class,
                LdbcQuery11.class,
                LdbcQuery12.class,
                LdbcQuery14.class
        );

        Integer operationTypeCount = enabledLongReadOperationTypes.size() + enabledWriteOperationTypes.size();
        long minimumResultCountPerOperationType = Math.max(
                1,
                Math.round(Math.floor(requiredValidationParameterCount.doubleValue() / operationTypeCount.doubleValue()))
        );

        long writeAddPersonOperationCount = (enabledWriteOperationTypes.contains(LdbcUpdate1AddPerson.class))
                ? minimumResultCountPerOperationType
                : 0;

        final Map<Class, Long> remainingRequiredResultsPerUpdateType = new HashMap<>();
        long resultCountsAssignedForUpdateTypesSoFar = 0;
        for (Class updateOperationType : enabledWriteOperationTypes) {
            if (updateOperationType.equals(LdbcUpdate1AddPerson.class)) continue;
            remainingRequiredResultsPerUpdateType.put(updateOperationType, minimumResultCountPerOperationType);
            resultCountsAssignedForUpdateTypesSoFar = resultCountsAssignedForUpdateTypesSoFar + minimumResultCountPerOperationType;
        }

        final Map<Class, Long> remainingRequiredResultsPerLongReadType = new HashMap<>();
        long resultCountsAssignedForLongReadTypesSoFar = 0;
        for (Class longReadOperationType : enabledLongReadOperationTypes) {
            remainingRequiredResultsPerLongReadType.put(longReadOperationType, minimumResultCountPerOperationType);
            resultCountsAssignedForLongReadTypesSoFar = resultCountsAssignedForLongReadTypesSoFar + minimumResultCountPerOperationType;
        }

        return new LdbcSnbInteractiveDbValidationParametersFilter(
                multiResultOperations,
                writeAddPersonOperationCount,
                remainingRequiredResultsPerUpdateType,
                remainingRequiredResultsPerLongReadType,
                enabledShortReadOperationTypes
        );
    }

    @Override
    public long maxExpectedInterleaveAsMilli() {
        return TimeUnit.HOURS.toMillis(1);
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference TYPE_REFERENCE = new TypeReference<List<Object>>() {
    };

    @Override
    public String serializeOperation(Operation operation) throws SerializingMarshallingException {
        switch (operation.type()) {
            case LdbcQuery1.TYPE: {
                LdbcQuery1 ldbcQuery = (LdbcQuery1) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.firstName());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery2.TYPE: {
                LdbcQuery2 ldbcQuery = (LdbcQuery2) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.maxDate().getTime());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery3.TYPE: {
                LdbcQuery3 ldbcQuery = (LdbcQuery3) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.countryXName());
                operationAsList.add(ldbcQuery.countryYName());
                operationAsList.add(ldbcQuery.startDate().getTime());
                operationAsList.add(ldbcQuery.durationDays());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery4.TYPE: {
                LdbcQuery4 ldbcQuery = (LdbcQuery4) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.startDate().getTime());
                operationAsList.add(ldbcQuery.durationDays());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery5.TYPE: {
                LdbcQuery5 ldbcQuery = (LdbcQuery5) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.minDate().getTime());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery6.TYPE: {
                LdbcQuery6 ldbcQuery = (LdbcQuery6) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.tagName());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery7.TYPE: {
                LdbcQuery7 ldbcQuery = (LdbcQuery7) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery8.TYPE: {
                LdbcQuery8 ldbcQuery = (LdbcQuery8) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery9.TYPE: {
                LdbcQuery9 ldbcQuery = (LdbcQuery9) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.maxDate().getTime());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery10.TYPE: {
                LdbcQuery10 ldbcQuery = (LdbcQuery10) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.month());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery11.TYPE: {
                LdbcQuery11 ldbcQuery = (LdbcQuery11) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.countryName());
                operationAsList.add(ldbcQuery.workFromYear());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery12.TYPE: {
                LdbcQuery12 ldbcQuery = (LdbcQuery12) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.tagClassName());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery13.TYPE: {
                LdbcQuery13 ldbcQuery = (LdbcQuery13) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.person1Id());
                operationAsList.add(ldbcQuery.person2Id());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcQuery14.TYPE: {
                LdbcQuery14 ldbcQuery = (LdbcQuery14) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.person1Id());
                operationAsList.add(ldbcQuery.person2Id());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcShortQuery1PersonProfile.TYPE: {
                LdbcShortQuery1PersonProfile ldbcQuery = (LdbcShortQuery1PersonProfile) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcShortQuery2PersonPosts.TYPE: {
                LdbcShortQuery2PersonPosts ldbcQuery = (LdbcShortQuery2PersonPosts) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.limit());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcShortQuery3PersonFriends.TYPE: {
                LdbcShortQuery3PersonFriends ldbcQuery = (LdbcShortQuery3PersonFriends) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcShortQuery4MessageContent.TYPE: {
                LdbcShortQuery4MessageContent ldbcQuery = (LdbcShortQuery4MessageContent) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.messageId());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcShortQuery5MessageCreator.TYPE: {
                LdbcShortQuery5MessageCreator ldbcQuery = (LdbcShortQuery5MessageCreator) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.messageId());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcShortQuery6MessageForum.TYPE: {
                LdbcShortQuery6MessageForum ldbcQuery = (LdbcShortQuery6MessageForum) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.messageId());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcShortQuery7MessageReplies.TYPE: {
                LdbcShortQuery7MessageReplies ldbcQuery = (LdbcShortQuery7MessageReplies) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.messageId());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcUpdate1AddPerson.TYPE: {
                LdbcUpdate1AddPerson ldbcQuery = (LdbcUpdate1AddPerson) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.personFirstName());
                operationAsList.add(ldbcQuery.personLastName());
                operationAsList.add(ldbcQuery.gender());
                operationAsList.add(ldbcQuery.birthday().getTime());
                operationAsList.add(ldbcQuery.creationDate().getTime());
                operationAsList.add(ldbcQuery.locationIp());
                operationAsList.add(ldbcQuery.browserUsed());
                operationAsList.add(ldbcQuery.cityId());
                operationAsList.add(ldbcQuery.languages());
                operationAsList.add(ldbcQuery.emails());
                operationAsList.add(ldbcQuery.tagIds());
                Iterable<Map<String, Object>> studyAt = Lists.newArrayList(Iterables.transform(ldbcQuery.studyAt(), new Function<LdbcUpdate1AddPerson.Organization, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> apply(LdbcUpdate1AddPerson.Organization organization) {
                        Map<String, Object> organizationMap = new HashMap<>();
                        organizationMap.put("id", organization.organizationId());
                        organizationMap.put("year", organization.year());
                        return organizationMap;
                    }
                }));
                operationAsList.add(studyAt);
                Iterable<Map<String, Object>> workAt = Lists.newArrayList(Iterables.transform(ldbcQuery.workAt(), new Function<LdbcUpdate1AddPerson.Organization, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> apply(LdbcUpdate1AddPerson.Organization organization) {
                        Map<String, Object> organizationMap = new HashMap<>();
                        organizationMap.put("id", organization.organizationId());
                        organizationMap.put("year", organization.year());
                        return organizationMap;
                    }
                }));
                operationAsList.add(workAt);
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcUpdate2AddPostLike.TYPE: {
                LdbcUpdate2AddPostLike ldbcQuery = (LdbcUpdate2AddPostLike) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.postId());
                operationAsList.add(ldbcQuery.creationDate().getTime());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcUpdate3AddCommentLike.TYPE: {
                LdbcUpdate3AddCommentLike ldbcQuery = (LdbcUpdate3AddCommentLike) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.commentId());
                operationAsList.add(ldbcQuery.creationDate().getTime());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcUpdate4AddForum.TYPE: {
                LdbcUpdate4AddForum ldbcQuery = (LdbcUpdate4AddForum) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.forumId());
                operationAsList.add(ldbcQuery.forumTitle());
                operationAsList.add(ldbcQuery.creationDate().getTime());
                operationAsList.add(ldbcQuery.moderatorPersonId());
                operationAsList.add(ldbcQuery.tagIds());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcUpdate5AddForumMembership.TYPE: {
                LdbcUpdate5AddForumMembership ldbcQuery = (LdbcUpdate5AddForumMembership) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.forumId());
                operationAsList.add(ldbcQuery.personId());
                operationAsList.add(ldbcQuery.joinDate().getTime());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcUpdate6AddPost.TYPE: {
                LdbcUpdate6AddPost ldbcQuery = (LdbcUpdate6AddPost) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.postId());
                operationAsList.add(ldbcQuery.imageFile());
                operationAsList.add(ldbcQuery.creationDate().getTime());
                operationAsList.add(ldbcQuery.locationIp());
                operationAsList.add(ldbcQuery.browserUsed());
                operationAsList.add(ldbcQuery.language());
                operationAsList.add(ldbcQuery.content());
                operationAsList.add(ldbcQuery.length());
                operationAsList.add(ldbcQuery.authorPersonId());
                operationAsList.add(ldbcQuery.forumId());
                operationAsList.add(ldbcQuery.countryId());
                operationAsList.add(ldbcQuery.tagIds());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcUpdate7AddComment.TYPE: {
                LdbcUpdate7AddComment ldbcQuery = (LdbcUpdate7AddComment) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.commentId());
                operationAsList.add(ldbcQuery.creationDate());
                operationAsList.add(ldbcQuery.locationIp());
                operationAsList.add(ldbcQuery.browserUsed());
                operationAsList.add(ldbcQuery.content());
                operationAsList.add(ldbcQuery.length());
                operationAsList.add(ldbcQuery.authorPersonId());
                operationAsList.add(ldbcQuery.countryId());
                operationAsList.add(ldbcQuery.replyToPostId());
                operationAsList.add(ldbcQuery.replyToCommentId());
                operationAsList.add(ldbcQuery.tagIds());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            case LdbcUpdate8AddFriendship.TYPE: {
                LdbcUpdate8AddFriendship ldbcQuery = (LdbcUpdate8AddFriendship) operation;
                List<Object> operationAsList = new ArrayList<>();
                operationAsList.add(ldbcQuery.getClass().getName());
                operationAsList.add(ldbcQuery.person1Id());
                operationAsList.add(ldbcQuery.person2Id());
                operationAsList.add(ldbcQuery.creationDate().getTime());
                try {
                    return OBJECT_MAPPER.writeValueAsString(operationAsList);
                } catch (IOException e) {
                    throw new SerializingMarshallingException(String.format("Error while trying to serialize result\n%s", operationAsList.toString()), e);
                }
            }
            default: {
                throw new SerializingMarshallingException(
                        String.format("Workload does not know how to serialize operation\nWorkload: %s\nOperation Type: %s\nOperation: %s",
                                getClass().getName(),
                                operation.getClass().getName(),
                                operation));
            }
        }
    }

    @Override
    public Operation marshalOperation(String serializedOperation) throws SerializingMarshallingException {
        List<Object> operationAsList;
        try {
            operationAsList = OBJECT_MAPPER.readValue(serializedOperation, TYPE_REFERENCE);
        } catch (IOException e) {
            throw new SerializingMarshallingException(String.format("Error while parsing serialized results\n%s", serializedOperation), e);
        }

        String operationTypeName = (String) operationAsList.get(0);

        if (operationTypeName.equals(LdbcQuery1.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            String firstName = (String) operationAsList.get(2);
            int limit = ((Number) operationAsList.get(3)).intValue();
            return new LdbcQuery1(personId, firstName, limit);
        }

        if (operationTypeName.equals(LdbcQuery2.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            Date maxDate = new Date(((Number) operationAsList.get(2)).longValue());
            int limit = ((Number) operationAsList.get(3)).intValue();
            return new LdbcQuery2(personId, maxDate, limit);
        }

        if (operationTypeName.equals(LdbcQuery3.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            String countryXName = (String) operationAsList.get(2);
            String countryYName = (String) operationAsList.get(3);
            Date startDate = new Date(((Number) operationAsList.get(4)).longValue());
            int durationDays = ((Number) operationAsList.get(5)).intValue();
            int limit = ((Number) operationAsList.get(6)).intValue();
            return new LdbcQuery3(personId, countryXName, countryYName, startDate, durationDays, limit);
        }

        if (operationTypeName.equals(LdbcQuery4.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            Date startDate = new Date(((Number) operationAsList.get(2)).longValue());
            int durationDays = ((Number) operationAsList.get(3)).intValue();
            int limit = ((Number) operationAsList.get(4)).intValue();
            return new LdbcQuery4(personId, startDate, durationDays, limit);
        }

        if (operationTypeName.equals(LdbcQuery5.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            Date minDate = new Date(((Number) operationAsList.get(2)).longValue());
            int limit = ((Number) operationAsList.get(3)).intValue();
            return new LdbcQuery5(personId, minDate, limit);
        }

        if (operationTypeName.equals(LdbcQuery6.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            String tagName = (String) operationAsList.get(2);
            int limit = ((Number) operationAsList.get(3)).intValue();
            return new LdbcQuery6(personId, tagName, limit);
        }

        if (operationTypeName.equals(LdbcQuery7.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            int limit = ((Number) operationAsList.get(2)).intValue();
            return new LdbcQuery7(personId, limit);
        }

        if (operationTypeName.equals(LdbcQuery8.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            int limit = ((Number) operationAsList.get(2)).intValue();
            return new LdbcQuery8(personId, limit);
        }

        if (operationTypeName.equals(LdbcQuery9.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            Date maxDate = new Date(((Number) operationAsList.get(2)).longValue());
            int limit = ((Number) operationAsList.get(3)).intValue();
            return new LdbcQuery9(personId, maxDate, limit);
        }

        if (operationTypeName.equals(LdbcQuery10.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            int month = ((Number) operationAsList.get(2)).intValue();
            int limit = ((Number) operationAsList.get(3)).intValue();
            return new LdbcQuery10(personId, month, limit);
        }

        if (operationTypeName.equals(LdbcQuery11.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            String countryName = (String) operationAsList.get(2);
            int workFromYear = ((Number) operationAsList.get(3)).intValue();
            int limit = ((Number) operationAsList.get(4)).intValue();
            return new LdbcQuery11(personId, countryName, workFromYear, limit);
        }

        if (operationTypeName.equals(LdbcQuery12.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            String tagClassName = (String) operationAsList.get(2);
            int limit = ((Number) operationAsList.get(3)).intValue();
            return new LdbcQuery12(personId, tagClassName, limit);
        }

        if (operationTypeName.equals(LdbcQuery13.class.getName())) {
            long person1Id = ((Number) operationAsList.get(1)).longValue();
            long person2Id = ((Number) operationAsList.get(2)).longValue();
            return new LdbcQuery13(person1Id, person2Id);
        }

        if (operationTypeName.equals(LdbcQuery14.class.getName())) {
            long person1Id = ((Number) operationAsList.get(1)).longValue();
            long person2Id = ((Number) operationAsList.get(2)).longValue();
            return new LdbcQuery14(person1Id, person2Id);
        }

        if (operationTypeName.equals(LdbcShortQuery1PersonProfile.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            return new LdbcShortQuery1PersonProfile(personId);
        }

        if (operationTypeName.equals(LdbcShortQuery2PersonPosts.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            int limit = ((Number) operationAsList.get(2)).intValue();
            return new LdbcShortQuery2PersonPosts(personId, limit);
        }

        if (operationTypeName.equals(LdbcShortQuery3PersonFriends.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            return new LdbcShortQuery3PersonFriends(personId);
        }

        if (operationTypeName.equals(LdbcShortQuery4MessageContent.class.getName())) {
            long messageId = ((Number) operationAsList.get(1)).longValue();
            return new LdbcShortQuery4MessageContent(messageId);
        }

        if (operationTypeName.equals(LdbcShortQuery5MessageCreator.class.getName())) {
            long messageId = ((Number) operationAsList.get(1)).longValue();
            return new LdbcShortQuery5MessageCreator(messageId);
        }

        if (operationTypeName.equals(LdbcShortQuery6MessageForum.class.getName())) {
            long messageId = ((Number) operationAsList.get(1)).longValue();
            return new LdbcShortQuery6MessageForum(messageId);
        }

        if (operationTypeName.equals(LdbcShortQuery7MessageReplies.class.getName())) {
            long messageId = ((Number) operationAsList.get(1)).longValue();
            return new LdbcShortQuery7MessageReplies(messageId);
        }

        if (operationTypeName.equals(LdbcUpdate1AddPerson.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            String personFirstName = (String) operationAsList.get(2);
            String personLastName = (String) operationAsList.get(3);
            String gender = (String) operationAsList.get(4);
            Date birthday = new Date(((Number) operationAsList.get(5)).longValue());
            Date creationDate = new Date(((Number) operationAsList.get(6)).longValue());
            String locationIp = (String) operationAsList.get(7);
            String browserUsed = (String) operationAsList.get(8);
            long cityId = ((Number) operationAsList.get(9)).longValue();
            List<String> languages = (List<String>) operationAsList.get(10);
            List<String> emails = (List<String>) operationAsList.get(11);
            List<Long> tagIds = Lists.newArrayList(Iterables.transform((List<Number>) operationAsList.get(12), new Function<Number, Long>() {
                @Override
                public Long apply(Number number) {
                    return number.longValue();
                }
            }));
            List<Map<String, Object>> studyAtList = (List<Map<String, Object>>) operationAsList.get(13);
            List<LdbcUpdate1AddPerson.Organization> studyAt = Lists.newArrayList(Iterables.transform(studyAtList, new Function<Map<String, Object>, LdbcUpdate1AddPerson.Organization>() {
                @Override
                public LdbcUpdate1AddPerson.Organization apply(Map<String, Object> input) {
                    long organizationId = ((Number) input.get("id")).longValue();
                    int year = ((Number) input.get("year")).intValue();
                    return new LdbcUpdate1AddPerson.Organization(organizationId, year);
                }
            }));
            List<Map<String, Object>> workAtList = (List<Map<String, Object>>) operationAsList.get(14);
            List<LdbcUpdate1AddPerson.Organization> workAt = Lists.newArrayList(Iterables.transform(workAtList, new Function<Map<String, Object>, LdbcUpdate1AddPerson.Organization>() {
                @Override
                public LdbcUpdate1AddPerson.Organization apply(Map<String, Object> input) {
                    long organizationId = ((Number) input.get("id")).longValue();
                    int year = ((Number) input.get("year")).intValue();
                    return new LdbcUpdate1AddPerson.Organization(organizationId, year);
                }
            }));

            return new LdbcUpdate1AddPerson(personId, personFirstName, personLastName, gender, birthday, creationDate,
                    locationIp, browserUsed, cityId, languages, emails, tagIds, studyAt, workAt);
        }

        if (operationTypeName.equals(LdbcUpdate2AddPostLike.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            long postId = ((Number) operationAsList.get(2)).longValue();
            Date creationDate = new Date(((Number) operationAsList.get(3)).longValue());

            return new LdbcUpdate2AddPostLike(personId, postId, creationDate);
        }

        if (operationTypeName.equals(LdbcUpdate3AddCommentLike.class.getName())) {
            long personId = ((Number) operationAsList.get(1)).longValue();
            long commentId = ((Number) operationAsList.get(2)).longValue();
            Date creationDate = new Date(((Number) operationAsList.get(3)).longValue());

            return new LdbcUpdate3AddCommentLike(personId, commentId, creationDate);
        }

        if (operationTypeName.equals(LdbcUpdate4AddForum.class.getName())) {
            long forumId = ((Number) operationAsList.get(1)).longValue();
            String forumTitle = (String) operationAsList.get(2);
            Date creationDate = new Date(((Number) operationAsList.get(3)).longValue());
            long moderatorPersonId = ((Number) operationAsList.get(4)).longValue();
            List<Long> tagIds = Lists.newArrayList(Iterables.transform((List<Number>) operationAsList.get(5), new Function<Number, Long>() {
                @Override
                public Long apply(Number number) {
                    return number.longValue();
                }
            }));

            return new LdbcUpdate4AddForum(forumId, forumTitle, creationDate, moderatorPersonId, tagIds);
        }


        if (operationTypeName.equals(LdbcUpdate5AddForumMembership.class.getName())) {
            long forumId = ((Number) operationAsList.get(1)).longValue();
            long personId = ((Number) operationAsList.get(2)).longValue();
            Date creationDate = new Date(((Number) operationAsList.get(3)).longValue());

            return new LdbcUpdate5AddForumMembership(forumId, personId, creationDate);
        }

        if (operationTypeName.equals(LdbcUpdate6AddPost.class.getName())) {
            long postId = ((Number) operationAsList.get(1)).longValue();
            String imageFile = (String) operationAsList.get(2);
            Date creationDate = new Date(((Number) operationAsList.get(3)).longValue());
            String locationIp = (String) operationAsList.get(4);
            String browserUsed = (String) operationAsList.get(5);
            String language = (String) operationAsList.get(6);
            String content = (String) operationAsList.get(7);
            int length = ((Number) operationAsList.get(8)).intValue();
            long authorPersonId = ((Number) operationAsList.get(9)).longValue();
            long forumId = ((Number) operationAsList.get(10)).longValue();
            long countryId = ((Number) operationAsList.get(11)).longValue();
            List<Long> tagIds = Lists.newArrayList(Iterables.transform((List<Number>) operationAsList.get(12), new Function<Number, Long>() {
                @Override
                public Long apply(Number number) {
                    return number.longValue();
                }
            }));

            return new LdbcUpdate6AddPost(postId, imageFile, creationDate, locationIp, browserUsed, language, content, length, authorPersonId, forumId, countryId, tagIds);
        }

        if (operationTypeName.equals(LdbcUpdate7AddComment.class.getName())) {
            long commentId = ((Number) operationAsList.get(1)).longValue();
            Date creationDate = new Date(((Number) operationAsList.get(2)).longValue());
            String locationIp = (String) operationAsList.get(3);
            String browserUsed = (String) operationAsList.get(4);
            String content = (String) operationAsList.get(5);
            int length = ((Number) operationAsList.get(6)).intValue();
            long authorPersonId = ((Number) operationAsList.get(7)).longValue();
            long countryId = ((Number) operationAsList.get(8)).longValue();
            long replyToPostId = ((Number) operationAsList.get(9)).longValue();
            long replyToCommentId = ((Number) operationAsList.get(10)).longValue();
            List<Long> tagIds = Lists.newArrayList(Iterables.transform((List<Number>) operationAsList.get(11), new Function<Number, Long>() {
                @Override
                public Long apply(Number number) {
                    return number.longValue();
                }
            }));

            return new LdbcUpdate7AddComment(commentId, creationDate, locationIp, browserUsed, content, length, authorPersonId, countryId, replyToPostId, replyToCommentId, tagIds);
        }

        if (operationTypeName.equals(LdbcUpdate8AddFriendship.class.getName())) {
            long person1Id = ((Number) operationAsList.get(1)).longValue();
            long person2Id = ((Number) operationAsList.get(2)).longValue();
            Date creationDate = new Date(((Number) operationAsList.get(3)).longValue());

            return new LdbcUpdate8AddFriendship(person1Id, person2Id, creationDate);
        }

        throw new SerializingMarshallingException(
                String.format("Workload does not know how to marshal operation\nWorkload: %s\nAssumed Operation Type: %s\nSerialized Operation: %s",
                        getClass().getName(),
                        operationTypeName,
                        serializedOperation));
    }
}
