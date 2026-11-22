package storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import logic.SimulationClock;
import model.*;

public class DataStore {
    private final String dataDir;

    public DataStore() {
        this("data");
    }

    public DataStore(String dataDir) {
        this.dataDir = dataDir;
    }

    private static String escape(String text) {
        if (text == null) { return ""; }
        return text.replace("\\", "\\\\")
                   .replace(",", "\\c")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }

    private static String unescape(String text) {
        if (text == null) { return ""; }
        return text.replace("\\n", "\n")
                   .replace("\\c", ",")
                   .replace("\\\\", "\\");
    }

    public void saveAll(AppState state) throws ProjectException {
        File dir = new File(dataDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new ProjectException("Could not create storage directory: " + dataDir);
        }
        saveClock(state);
        saveStudents(state);
        saveRequests(state);
        saveJobs(state);
        saveGrants(state);
    }

    private void saveClock(AppState state) throws ProjectException {
        File file = new File(dataDir, "clock.csv");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            out.println("initial,now,nextJobNumber");
            SimulationClock clock = state.getClock();
            out.println(clock.getInitial() + "," + clock.getNow() + "," + state.getNextJobNumber());
        } catch (IOException e) {
            throw new ProjectException("Could not write " + file.getPath() + ": " + e.getMessage());
        }
    }

    private void saveStudents(AppState state) throws ProjectException {
        File file = new File(dataDir, "students.csv");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            out.println("username,password,name,email,phone,allocated,used,reserved");
            for (Student s : state.getStudents()) {
                TimeAllowance a = s.getAllowance();
                out.println(escape(s.getUsername()) + "," +
                            escape(s.getPassword()) + "," +
                            escape(s.getName()) + "," +
                            escape(s.getEmail()) + "," +
                            escape(s.getPhone()) + "," +
                            a.getAllocated() + "," +
                            a.getUsed() + "," +
                            a.getReserved());
            }
        } catch (IOException e) {
            throw new ProjectException("Could not write " + file.getPath() + ": " + e.getMessage());
        }
    }

    private void saveRequests(AppState state) throws ProjectException {
        File file = new File(dataDir, "requests.csv");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            out.println("id,name,email,phone,password,status");
            for (AccessRequest r : state.getRequests()) {
                out.println(escape(r.getId()) + "," +
                            escape(r.getName()) + "," +
                            escape(r.getEmail()) + "," +
                            escape(r.getPhone()) + "," +
                            escape(r.getPassword()) + "," +
                            escape(r.getStatus()));
            }
        } catch (IOException e) {
            throw new ProjectException("Could not write " + file.getPath() + ": " + e.getMessage());
        }
    }

    private void saveJobs(AppState state) throws ProjectException {
        File file = new File(dataDir, "jobs.csv");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            out.println("number,studentId,title,application,description,duration,submissionTime,status,gpuId,startTime,finishTime,cancellationTime,cancelledBy,cancellationReason");
            for (Job j : state.getJobs()) {
                out.println(j.getNumber() + "," +
                            escape(j.getStudentId()) + "," +
                            escape(j.getTitle()) + "," +
                            escape(j.getApplication()) + "," +
                            escape(j.getDescription()) + "," +
                            j.getDuration() + "," +
                            j.getSubmissionTime() + "," +
                            escape(j.getStatus()) + "," +
                            j.getGpuId() + "," +
                            j.getStartTime() + "," +
                            j.getFinishTime() + "," +
                            j.getCancellationTime() + "," +
                            escape(j.getCancelledBy()) + "," +
                            escape(j.getCancellationReason()));
            }
        } catch (IOException e) {
            throw new ProjectException("Could not write " + file.getPath() + ": " + e.getMessage());
        }
    }

    private void saveGrants(AppState state) throws ProjectException {
        File file = new File(dataDir, "grants.csv");
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            out.println("entry");
            for (String log : state.getTimeGrantLogs()) {
                out.println(escape(log));
            }
        } catch (IOException e) {
            throw new ProjectException("Could not write " + file.getPath() + ": " + e.getMessage());
        }
    }

    public AppState loadAll() throws ProjectException {
        AppState state = new AppState();
        File dir = new File(dataDir);
        if (!dir.exists()) {
            return state;
        }
        loadClock(state);
        loadStudents(state);
        loadRequests(state);
        loadJobs(state);
        loadGrants(state);
        return state;
    }

    private void loadClock(AppState state) throws ProjectException {
        File file = new File(dataDir, "clock.csv");
        if (!file.exists()) { return; }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) { return; }
            String line = br.readLine();
            if (line == null || line.trim().isEmpty()) { return; }
            String[] parts = line.split(",", -1);
            if (parts.length < 3) {
                throw new ProjectException("Could not read " + file.getPath() + " (line 2: expected 3 columns).");
            }
            long initial = Long.parseLong(parts[0].trim());
            long now = Long.parseLong(parts[1].trim());
            int nextJob = Integer.parseInt(parts[2].trim());
            state.setClock(new SimulationClock(initial, now));
            state.setNextJobNumber(nextJob);
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException("Could not read " + file.getPath() + " (line 2: " + e.getMessage() + ").");
        }
    }

    private void loadStudents(AppState state) throws ProjectException {
        File file = new File(dataDir, "students.csv");
        if (!file.exists()) { return; }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            int lineNum = 1;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) { continue; }
                String[] p = line.split(",", -1);
                if (p.length < 8) {
                    throw new ProjectException("Could not read " + file.getPath() + " (line " + lineNum + ": missing columns).");
                }
                String username = unescape(p[0].trim());
                String password = unescape(p[1].trim());
                String name = unescape(p[2].trim());
                String email = unescape(p[3].trim());
                String phone = unescape(p[4].trim());
                int allocated = Integer.parseInt(p[5].trim());
                int used = Integer.parseInt(p[6].trim());
                int reserved = Integer.parseInt(p[7].trim());
                if (allocated < 0 || used < 0 || reserved < 0) {
                    throw new ProjectException("Could not read " + file.getPath() + " (line " + lineNum + ": negative balance).");
                }
                TimeAllowance allowance = new TimeAllowance(allocated, used, reserved);
                state.addStudent(new Student(username, password, name, email, phone, allowance));
            }
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException("Could not read " + file.getPath() + " (" + e.getMessage() + ").");
        }
    }

    private void loadRequests(AppState state) throws ProjectException {
        File file = new File(dataDir, "requests.csv");
        if (!file.exists()) { return; }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            int lineNum = 1;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) { continue; }
                String[] p = line.split(",", -1);
                if (p.length < 6) {
                    throw new ProjectException("Could not read " + file.getPath() + " (line " + lineNum + ": missing columns).");
                }
                AccessRequest req = new AccessRequest(
                    unescape(p[0].trim()),
                    unescape(p[1].trim()),
                    unescape(p[2].trim()),
                    unescape(p[3].trim()),
                    unescape(p[4].trim()),
                    unescape(p[5].trim())
                );
                state.addRequest(req);
            }
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException("Could not read " + file.getPath() + " (" + e.getMessage() + ").");
        }
    }

    private void loadJobs(AppState state) throws ProjectException {
        File file = new File(dataDir, "jobs.csv");
        if (!file.exists()) { return; }
        HashMap<Integer, Job> runningOnGpu = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            int lineNum = 1;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) { continue; }
                String[] p = line.split(",", -1);
                if (p.length < 14) {
                    throw new ProjectException("Could not read " + file.getPath() + " (line " + lineNum + ": missing columns).");
                }
                int number = Integer.parseInt(p[0].trim());
                String studentId = unescape(p[1].trim());
                String title = unescape(p[2].trim());
                String application = unescape(p[3].trim());
                String description = unescape(p[4].trim());
                int duration = Integer.parseInt(p[5].trim());
                long submissionTime = Long.parseLong(p[6].trim());
                String status = unescape(p[7].trim());
                int gpuId = Integer.parseInt(p[8].trim());
                long startTime = Long.parseLong(p[9].trim());
                long finishTime = Long.parseLong(p[10].trim());
                long cancelTime = Long.parseLong(p[11].trim());
                String cancelledBy = unescape(p[12].trim());
                String cancelReason = unescape(p[13].trim());

                if (!Job.validDuration(duration)) {
                    throw new ProjectException("Could not read " + file.getPath() + " (line " + lineNum + ": invalid duration " + duration + ").");
                }

                Job job = new Job(number, studentId, title, application, description, duration,
                                  submissionTime, status, gpuId, startTime, finishTime,
                                  cancelTime, cancelledBy, cancelReason);
                state.addJob(job);

                if (Job.RUNNING.equals(status)) {
                    if (gpuId < 1 || gpuId > 4 || runningOnGpu.containsKey(gpuId)) {
                        throw new ProjectException("Could not read " + file.getPath() + " (line " + lineNum + ": invalid or duplicate GPU assignment).");
                    }
                    runningOnGpu.put(gpuId, job);
                }
            }
            for (Integer gpuId : runningOnGpu.keySet()) {
                state.getGpu(gpuId).assign(runningOnGpu.get(gpuId));
            }
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException("Could not read " + file.getPath() + " (" + e.getMessage() + ").");
        }
    }

    private void loadGrants(AppState state) throws ProjectException {
        File file = new File(dataDir, "grants.csv");
        if (!file.exists()) { return; }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    state.addTimeGrantLog(unescape(line.trim()));
                }
            }
        } catch (Exception e) {
            throw new ProjectException("Could not read " + file.getPath() + " (" + e.getMessage() + ").");
        }
    }
}