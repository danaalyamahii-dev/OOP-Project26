# GPU Utilization Management — COSC230

A Java Swing application that simulates scheduling student jobs on four NVIDIA H100 GPUs. It uses a simulated clock and saves application data in CSV files.

## Requirements

- Java Development Kit (JDK)
- Eclipse IDE for Java Developers
- No external libraries are required by the source code.

## How to run in Eclipse

1. Extract the project ZIP.
2. In Eclipse, choose **File > Import > General > Existing Projects into Workspace**.
3. Select the extracted **Project** folder and finish importing.
4. In **Package Explorer**, open `src > gpu > Main.java`.
5. Right-click `Main.java` and choose **Run As > Java Application**.
6. If prompted to set the working directory, use the **Project** folder. The program reads and writes a relative `data` directory there.

If Eclipse says the project already exists, use a separate workspace or rename the copied Eclipse project before importing.

## Login and first run

- Administrator username: `admin`
- Administrator password: `gpu2026`
- Students request an account using their university ID, name, email, phone number, and password.
- The administrator must approve a pending request before the student can sign in.
- On the first run without a saved clock, the administrator sets the simulation start date and time. Afterward, time advances through **+1 minute**.

## Main features

**Students:** view allocated, used, reserved, and available GPU minutes; submit jobs with title, application, description, and task size; view and filter their jobs; cancel eligible jobs.

**Administrator:** review registration requests; approve or reject accounts; view GPU and job status; search students; grant extra GPU minutes with a reason; cancel eligible jobs.

**Scheduling:** four simulated GPUs, first-come-first-served (FCFS) queue, predicted waiting/start/finish times, and job statuses (queued, running, completed, cancelled).

## Saved data

The application uses `new DataStore("data")`. Its CSV files are stored relative to the application's **working directory**:

- `data/clock.csv` — simulated clock
- `data/students.csv` — student accounts and allowances
- `data/requests.csv` — registration requests
- `data/jobs.csv` — submitted jobs and statuses
- `data/grants.csv` — extra-time grants

The `data` directory is created when saving if necessary. To preserve a completed demonstration, close the program and copy the whole `data` folder to a safe location before resetting or testing other scenarios. Existing `state.txt` files from older versions are not read by this CSV-based version.

## Suggested demonstration

1. Submit a student registration request and verify the Pending message.
2. Sign in as administrator, initialize the clock if needed, and approve the request.
3. Sign in as the student and submit a job.
4. Submit the guide's 10-job example, advancing the simulated clock as required.
5. Check the four GPU panels, queue, completed jobs, balances, and predictions.
6. Demonstrate queued-job cancellation and an administrator time grant.
7. Save and exit, reopen, and check that saved state is restored.

## Source structure

- `src/gpu/Main.java` — application entry point
- `src/gpu/model/` — student, administrator, job, GPU, allowance, requests, and state classes
- `src/gpu/logic/` — account management, scheduler, predictions, and simulated clock
- `src/gpu/ui/` — Java Swing login, registration, student, and administrator screens
- `src/gpu/storage/DataStore.java` — CSV save/load

`reference_only/` contains legacy/reference files and is not part of the active application flow.

## Group and GitHub details

Add the final repository link and each member's GitHub username to the project report before submission. Submit the complete Java source, sample data, README, and report according to the COSC230 project guide and Blackboard instructions.
