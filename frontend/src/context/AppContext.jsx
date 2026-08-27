import { createContext, useContext, useState, useEffect, useCallback } from "react";
import api from "../api";
import { formatStoryDetails } from "../utils/story";

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [page, setPage] = useState("story");
  const [issueKey, setIssueKey] = useState("KAN-1");
  const [story, setStory] = useState(null);
  const [storyText, setStoryText] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [publishMessage, setPublishMessage] = useState(null);
  const [linkMessage, setLinkMessage] = useState(null);

  const [review, setReview] = useState(null);
  const [testCases, setTestCases] = useState([]);
  const [expandedCase, setExpandedCase] = useState(0);
  const [comment, setComment] = useState("");

  const [testCount, setTestCount] = useState(5);
  const [testType, setTestType] = useState("Functional");
  const [promptType, setPromptType] = useState("default");
  const [customPrompt, setCustomPrompt] = useState("");

  const [projects, setProjects] = useState({});
  const [folders, setFolders] = useState({});
  const [selectedProject, setSelectedProject] = useState("");
  const [selectedFolder, setSelectedFolder] = useState("");
  const [projectError, setProjectError] = useState(null);
  const [folderError, setFolderError] = useState(null);
  const [folderWarning, setFolderWarning] = useState(null);

  const [crKey, setCrKey] = useState("KAN-1");
  const [cycleTypes, setCycleTypes] = useState(["Functional"]);
  const [release, setRelease] = useState(null);
  const [cycleFetch, setCycleFetch] = useState(null);
  const [cyclesCreated, setCyclesCreated] = useState(null);
  const [cycleCreateMessage, setCycleCreateMessage] = useState(null);
  const [cycleLinkMessage, setCycleLinkMessage] = useState(null);
  const [releaseAi, setReleaseAi] = useState(null);
  const [aiActions, setAiActions] = useState([]);

  const [publishedKeys, setPublishedKeys] = useState([]);
  const [publishedTestCaseLinks, setPublishedTestCaseLinks] = useState([]);
  const [linkIssueKeys, setLinkIssueKeys] = useState("");
  const [cycleLinkKeys, setCycleLinkKeys] = useState("");

  const [connStatus, setConnStatus] = useState(null);
  const [configStatus, setConfigStatus] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [setupGuide, setSetupGuide] = useState(null);
  const [showSetupGuide, setShowSetupGuide] = useState(false);

  const owner = currentUser?.owner || currentUser?.accountId || currentUser?.emailAddress || "";
  const ownerName = currentUser?.displayName || owner || "Current user";
  const defaultStatus = configStatus?.zephyr?.defaultTestCaseStatus || testCases[0]?.status || "Approved";

  const clearMsg = () => { setError(null); setSuccess(null); };

  const navigate = (id) => { setPage(id); clearMsg(); };

  const updateTestCase = (index, field, value) => {
    setTestCases((prev) => prev.map((tc, i) => (i === index ? { ...tc, [field]: value } : tc)));
  };

  const updateStep = (tcIndex, stepIndex, value) => {
    setTestCases((prev) => prev.map((tc, i) => {
      if (i !== tcIndex) return tc;
      const steps = [...(tc.steps || [])];
      steps[stepIndex] = value;
      return { ...tc, steps };
    }));
  };

  const addStep = (tcIndex) => {
    setTestCases((prev) => prev.map((tc, i) => (
      i === tcIndex ? { ...tc, steps: [...(tc.steps || []), ""] } : tc
    )));
  };

  const removeStep = (tcIndex, stepIndex) => {
    setTestCases((prev) => prev.map((tc, i) => {
      if (i !== tcIndex) return tc;
      return { ...tc, steps: (tc.steps || []).filter((_, j) => j !== stepIndex) };
    }));
  };

  const removeTestCase = (index) => {
    setTestCases((prev) => prev.filter((_, i) => i !== index));
    setExpandedCase((current) => (current >= index && current > 0 ? current - 1 : current));
  };

  const run = useCallback(async (fn, onError) => {
    clearMsg();
    setLoading(true);
    try {
      await fn();
    } catch (e) {
      setError(e.message);
      if (onError) onError(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (story) setStoryText(formatStoryDetails(story));
  }, [story]);

  useEffect(() => {
    api.config.me()
      .then(setCurrentUser)
      .catch(() => setCurrentUser({ displayName: "Not signed in", emailAddress: "", success: false }));
    api.config.setupGuide().then(setSetupGuide).catch(() => {});
  }, []);

  useEffect(() => {
    setProjectError(null);
    api.zephyr.projects()
      .then((res) => {
        const map = res.projects ?? {};
        if (!res.success && res.error) setProjectError(res.error);
        if (res.success && !Object.keys(map).length) setProjectError("No projects found");
        setProjects(map);
        const ids = Object.values(map);
        if (ids.length > 0) setSelectedProject((current) => current || ids[0]);
      })
      .catch((e) => {
        setProjectError(e.message);
        setProjects({});
      });
  }, []);

  useEffect(() => {
    if (!selectedProject) return;
    api.zephyr.folders(selectedProject)
      .then((res) => {
        const map = res.folders ?? {};
        setFolders(map);
        setFolderError(!res.success ? (res.error || "Unable to resolve project folders") : null);
        setFolderWarning(res.success ? (res.warning || (!Object.keys(map).length ? "No folders found for this project" : null)) : null);
        setSelectedFolder(Object.values(map)[0] ?? "");
      })
      .catch((e) => {
        setFolders({});
        setFolderError(e.message || "Unable to resolve project folders");
        setFolderWarning(null);
      });
  }, [selectedProject]);

  useEffect(() => {
    api.release.aiActions()
      .then((res) => setAiActions(res.actions ?? []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    api.config.status()
      .then((data) => {
        setConfigStatus(data);
        const key = data?.zephyr?.defaultProjectKey;
        const defaultId = data?.zephyr?.defaultProjectId;
        if (key) {
          setIssueKey((current) => (current === "KAN-1" || current === "PROJ-1" ? `${key}-1` : current));
          setCrKey((current) => (current === "KAN-1" || current === "CR-1" ? `${key}-1` : current));
        }
        if (defaultId) setSelectedProject((current) => current || defaultId);
      })
      .catch(() => {});
  }, []);

  const fetchStory = () => run(async () => {
    const data = await api.jira.fetchIssue(issueKey);
    setStory(data);
    setLinkIssueKeys(issueKey.trim().toUpperCase());
    try {
      const project = await api.config.jiraProject(issueKey.split("-")[0]);
      setSelectedProject(project.id);
    } catch { /* The normal configured project remains selected. */ }
    setSuccess(`Fetched ${issueKey}`);
  });

  const fetchForAi = () => run(async () => {
    const data = await api.jira.fetchIssue(issueKey);
    setStory(data);
    try {
      const cr = await api.release.fetchCr(issueKey);
      setRelease(cr);
      setCrKey(issueKey);
    } catch {
      setRelease(null);
    }
    setLinkIssueKeys(issueKey.trim().toUpperCase());
    try {
      const project = await api.config.jiraProject(issueKey.split("-")[0]);
      setSelectedProject(project.id);
    } catch { /* The normal configured project remains selected. */ }
    setSuccess(`Fetched Jira details for ${issueKey}`);
  });

  const postComment = () => run(async () => {
    await api.jira.comment(issueKey, comment);
    setSuccess("Comment posted to Jira");
    setComment("");
  });

  const postAiComment = (commentText) => run(async () => {
    await api.jira.comment(issueKey, commentText);
    setSuccess(`AI result posted as a Jira comment on ${issueKey}`);
  });

  const doGenerate = () => run(async () => {
    const count = Math.max(1, Number(testCount) || 1);
    const data = await api.generate({
      issueKey,
      testType,
      testCount: count,
      promptType,
      customPrompt: promptType === "custom" ? customPrompt : undefined,
      jiraDetails: storyText || undefined,
    });
    setTestCases((data.testCases ?? []).map((tc) => ({
      ...tc,
      status: tc.status || defaultStatus,
      priority: tc.priority || "Normal",
      steps: tc.steps?.length ? [...tc.steps] : [""],
    })));
    setExpandedCase(0);
    setSuccess(`Generated ${data.testCases?.length ?? 0} ${testType} test case(s). Edit any field before publishing.`);
  });

  const publishAll = () => run(async () => {
    if (!testCases.length) throw new Error("Generate test cases first");
    if (!selectedProject) throw new Error("Select a project");
    if (!selectedFolder) throw new Error("Select a folder");

    const published = [];
    const publishedLinks = [];
    for (const tc of testCases) {
      const res = await api.zephyr.publish({
        testCase: tc,
        projectId: selectedProject,
        folderId: selectedFolder,
        owner,
        statusId: "",
      });
      if (res.testCaseKey) {
        published.push(res.testCaseKey);
        publishedLinks.push({ key: res.testCaseKey, url: res.testCaseUrl });
      }
    }
    setPublishedKeys(published);
    setPublishedTestCaseLinks(publishedLinks);
    setLinkIssueKeys((current) => current || issueKey);
    setPublishMessage({ type: "success", text: `Published ${published.length} test case(s): ${published.join(", ")}.` });
    setSuccess(`Published ${published.length} test case(s): ${published.join(", ")}. Link them to stories next.`);
  }, (e) => setPublishMessage({ type: "error", text: e.message }));

  const linkPublished = () => run(async () => {
    if (!publishedKeys.length) throw new Error("Publish test cases first");
    const keys = linkIssueKeys.split(/[,\s]+/).map((k) => k.trim()).filter(Boolean);
    if (!keys.length) throw new Error("Enter at least one Jira story or change-ticket key");
    const res = await api.zephyr.linkBulk(publishedKeys, keys);
    if (res.errors?.length && !res.linked?.length) throw new Error(res.errors.join("; "));
    if (res.errors?.length) setLinkMessage({ type: "error", text: `Some links need attention: ${res.errors.join("; ")}` });
    else setLinkMessage({ type: "success", text: `Linked ${res.linked?.length ?? 0} new link(s); ${res.alreadyLinked?.length ?? 0} were already linked.` });
    setSuccess(`Linked ${res.linked?.length ?? 0} new link(s); ${res.alreadyLinked?.length ?? 0} were already linked.`);
  }, (e) => setLinkMessage({ type: "error", text: e.message }));

  const fetchCycles = () => run(async () => {
    // A new fetch starts a new cycle workflow. Do not leave results or
    // controls from the previously selected story visible.
    setRelease(null);
    setCycleFetch(null);
    setCyclesCreated(null);
    setCycleCreateMessage(null);
    setCycleLinkMessage(null);
    setCycleLinkKeys("");
    const cr = await api.release.fetchCr(crKey);
    setRelease(cr);
    const cycles = await api.release.getTestCycles(crKey);
    setCycleFetch(cycles);
    const stories = (cr.linkedStories ?? []).map((s) => s.key).filter(Boolean);
    setCycleLinkKeys([crKey, ...stories].filter(Boolean).join(", "));
    setSuccess(cycles.message ?? `Fetched test cycles for ${crKey}`);
  });

  const createCycles = () => run(async () => {
    setCycleCreateMessage(null);
    if (!cycleFetch) throw new Error("Fetch test cycles first");
    if (!cycleTypes.length) throw new Error("Select at least one cycle type");
    const data = await api.release.createTestCycles(crKey, true, cycleTypes, owner, publishedKeys);
    setCyclesCreated(data);
    setCycleCreateMessage({ type: "success", text: data.message ?? `Created ${data.cyclesCreated} cycle(s)` });
  }, (e) => setCycleCreateMessage({ type: "error", text: e.message }));

  const linkCycles = () => run(async () => {
    setCycleLinkMessage(null);
    if (!cyclesCreated) throw new Error("Create test cycles first");
    const keys = cycleLinkKeys.split(/[,\s]+/).map((k) => k.trim()).filter(Boolean);
    if (!keys.length) throw new Error("Enter story / change-ticket keys to link");
    const cases = await api.release.getTestCases(crKey);
    const testKeys = [];
    (cases.storyTestCycles ?? []).forEach((s) => {
      (s.testCycles ?? []).forEach((cycle) => {
        (cycle.testCases ?? []).forEach((tc) => {
          if (tc.key) testKeys.push(tc.key);
        });
      });
    });
    const unique = [...new Set([...publishedKeys, ...testKeys])];
    if (!unique.length) {
      setCycleLinkMessage({
        type: "info",
        text: `No published test case keys found yet. Stories/CRs noted: ${keys.join(", ")}. Publish tests first, then link.`,
      });
      return;
    }
    const res = await api.zephyr.linkBulk(unique, keys);
    if (res.errors?.length && !res.linked?.length) throw new Error(res.errors.join("; "));
    if (res.errors?.length) setCycleLinkMessage({ type: "error", text: `Some links need attention: ${res.errors.join("; ")}` });
    else setCycleLinkMessage({ type: "success", text: `Linked ${res.linked?.length ?? 0} new link(s); ${res.alreadyLinked?.length ?? 0} were already linked.` });
  }, (e) => setCycleLinkMessage({ type: "error", text: e.message }));

  const releaseAiRun = (actionId) => run(async () => {
    if (actionId === "story-review") {
      const data = await api.aiReview({ issueKey, jiraDetails: storyText || undefined });
      setReview(data);
      setSuccess(data.mockMode ? "AI review (mock mode)" : "AI review complete");
      return;
    }
    const data = await api.release.aiAnalysis({ crKey: crKey || issueKey, action: actionId });
    setReleaseAi(data);
    setSuccess(data.mockMode ? "Release AI (mock mode)" : "Release analysis complete");
  });

  const testConnections = () => run(async () => {
    const [jira, user] = await Promise.all([
      api.jira.test().catch((e) => e.message),
      api.zephyr.validateUser().catch((e) => ({ error: e.message })),
    ]);
    setConnStatus({ jira, user });
    setSuccess("Connection check complete");
  });

  const toggleCycleType = (type) => {
    setCycleTypes((current) => (
      current.includes(type) ? current.filter((t) => t !== type) : [...current, type]
    ));
  };

  const value = {
    page, navigate,
    issueKey, setIssueKey,
    story, storyText,
    loading, error, success,
    publishMessage, linkMessage,
    review, releaseAi, aiActions,
    testCases, expandedCase, setExpandedCase,
    updateTestCase, updateStep, addStep, removeStep, removeTestCase,
    comment, setComment,
    testCount, setTestCount,
    testType, setTestType,
    promptType, setPromptType,
    customPrompt, setCustomPrompt,
    projects, folders, selectedProject, setSelectedProject,
    selectedFolder, setSelectedFolder,
    projectError, folderWarning,
    folderError,
    crKey, setCrKey,
    cycleTypes, toggleCycleType,
    release, cycleFetch, cyclesCreated,
    cycleCreateMessage, cycleLinkMessage,
    publishedKeys, linkIssueKeys, setLinkIssueKeys,
    publishedTestCaseLinks,
    cycleLinkKeys, setCycleLinkKeys,
    connStatus, configStatus, currentUser,
    setupGuide, showSetupGuide, setShowSetupGuide,
    owner, ownerName, defaultStatus,
    fetchStory, fetchForAi, postComment, postAiComment, doGenerate,
    publishAll, linkPublished,
    fetchCycles, createCycles, linkCycles,
    releaseAiRun, testConnections,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error("useApp must be used within AppProvider");
  return ctx;
}
