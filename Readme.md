Project Structure
--
Project has 3 modules
- Randomizer (Relays prime number to PrimeChecker process)
- PrimeChecker (Checks if the number is prime and sends back repsonse to Randomizer)
- Common (Has the frameworks classes like MMapFile, MMapFileReader, MMapFileWriter,MMapMessage etc)

System Requirements
--
- The program execution has been tested in Windows 8 (4 core, 8 GB ram) with upto 0.5 million prime numbers checked successfully.
- Please ensure to have write ability to "C:" drive (All memory map files are formed inisde C:\Temp, however is the Temp folder is not present in C: drive, the program will create it) 

Command to build the project
--
>mvn clean compile install
>> This command will create two runnable jars in the location _<project-dir>/Randomizer/target/Randomizer-1.0-SNAPSHOT-jar-with-dependencies.jar_ and _<project-dir>/PrimeChecker/PrimeChecker-1.0-SNAPSHOT-jar-with-dependencies.jar_

Run PrimeChecker
--
>Go to the directory _<project-dir>/PrimeChecker/target_
1) Launch the command prompt
2) Run the command "java -jar PrimeChecker-1.0-SNAPSHOT-jar-with-dependencies.jar"
>This will start the PrimeChecker process

Run Randomizer
--
>Go to the directory _<project-dir>/Randomizer/target_
1) Launch the command prompt
2) Run the command "java -jar Randomizer-1.0-SNAPSHOT-jar-with-dependencies.jar -n 1000" (Number of primes to be generated is passed as an argument to the program execution)
>This will start the Randomizer process and start relaying 1000 random numbers to which PrimeCheker process will reply back with the response whether the numbers are prime or not

Design
--
1) Randomizer process takes an input argument of number of random numbers to relay.
2) Randomizer process runs two threads "PrimeProducer" which relays "Input" number of random integers via a MemoryMappedFile (PrimeRequests) to the "PrimeChecker" process and "PrimerResponseConsumer" which reads back the responses from a response MemoryMapFile(PrimeResponses) to display the responses on to the standard output.
3) Prime Checker runs on the two memory mapped files
- PrimeRequests - Memory map file to hold requests
- PrimeResponses - Memory map file to hold responses (if the number is prime or not)
4) Prime Checker has 3 executor services
- PopulateRequestBufferTask - To populate the requests from MemoryMap File to the ConcurrentQueue
- CheckPrimePopulateResponseBufferTask - To take the request from requests concurrentqueue, calculate if the number is prime or not and populate the response concurrent queue
- PopulateRepsonseMMapTask - To populate the response mMemoryMap File from the respnse concurrenqueue

Advantages of Design
--
1) Usage of MemoryMap File makes the inter process communication quite fast.
2) The desing of MemoryMap file for the time being is MPSC (Multi Producer Single Consumer). Hence the introduction of concurrent queue to make the intermediate calculations multi threaded.
3) The use of concurrent queue makes the processing lock free and hence reduces latency and increases throughput.

Tests
--
- Currently there are unit tests present for the "Common" module and will be extended to other modules later
- Program is able to execute 0.5 million prime number check under 2 secs (Prime number range is 0 to 1M) 

Prosposed Enhancements
--
- The MemoryMap File can be abstracted as MPMC structure where we could directly read and write to MemoryMap File rather than introducing concurrent queue as an internal layer creating complications.
- Lock Free RingBuffer can be added to MemoryMap file to enhance the throughput and reduce latency of the operations.
- We can use third party libraries like Chronicle queue or LMAX Disruptor for the above scenarios
- All executable jars could be created in single "bin" folder within the project by enhancing maven pom.xml
- Unit test case coverage should be increases
- Performance tests should be added
- A better disctributed Prime Number check algo can be implemented.
- Configurability of the system (Configuring memory map file folder, memory map file size, record size) using xml.