import { createContext, useContext, useState, useEffect, useCallback } from "react";
import api from "../api";
import { formatStoryDetails } from "../utils/story";
import {
  deletePromptTemplate,
  loadSavedPromptTemplates,
  savePromptTemplate,
} from "../utils/promptTemplates";
import { loadAppStats, recordAppStat, resetAppStats } from "../utils/appStats";

const AppContext = createContext(null);

function resolveGenerationPrompt(promptType, customPrompt, savedTemplates, additionalPrompt) {
  if (promptType.startsWith("saved:")) {
    const templateId = promptType.slice("saved:".length);
    const template = savedTemplates.find((item) => item.id === templateId);
    return {
      promptType: "template",
      customPrompt: template?.prompt || "",
      additionalPrompt: additionalPrompt?.trim() || undefined,
    };
  }
  if (promptType === "custom") {
    return {
      promptType: "custom",
      customPrompt: customPrompt.trim() || undefined,
      additionalPrompt: additionalPrompt?.trim() || undefined,
    };
  }
  return {
    promptType,
    customPrompt: undefined,
    additionalPrompt: additionalPrompt?.trim() || undefined,
  };
}

function isSupportedAttachment(attachment) {
  const mime = attachment?.mimeType || "";
  return mime.startsWith("image/") || mime.startsWith("text/") || ["application/json", "text/csv"].includes(mime) || /\.(txt|md|csv|json)$/i.test(attachment?.name || "");
}

function updatePublishItem(items, index, patch) {
  return items.map((item) => (item.index === index ? { ...item, ...patch } : item));
}

function mergePublishItems(existingItems, nextItems) {
  const merged = new Map(existingItems.map((item) => [item.index, item]));
  nextItems.forEach((item) => {
    merged.set(item.index, { ...merged.get(item.index), ...item });
  });
  return [...merged.values()].sort((a, b) => a.index - b.index);
}

export function AppProvider({ children }) {
  const [page, setPage] = useState("home");
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
  const [additionalPrompt, setAdditionalPrompt] = useState("");
  const [aiLoading, setAiLoading] = useState(false);
  const [savedPromptTemplates, setSavedPromptTemplates] = useState([]);
  const [appStats, setAppStats] = useState(() => loadAppStats());
  const [publishing, setPublishing] = useState(false);
  const [publishProgress, setPublishProgress] = useState(null);

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

  const refreshSavedPromptTemplates = useCallback(() => {
    setSavedPromptTemplates(loadSavedPromptTemplates());
  }, []);

  useEffect(() => {
    refreshSavedPromptTemplates();
  }, [refreshSavedPromptTemplates]);

  const saveCurrentPromptTemplate = useCallback((name, prompt) => {
    const saved = savePromptTemplate({ name, prompt });
    refreshSavedPromptTemplates();
    setPromptType(`saved:${saved.id}`);
    setCustomPrompt(saved.prompt);
    setSuccess(`Saved prompt template "${saved.name}"`);
  }, [refreshSavedPromptTemplates]);

  const removePromptTemplate = useCallback((id) => {
    deletePromptTemplate(id);
    refreshSavedPromptTemplates();
    setPromptType((current) => (current === `saved:${id}` ? "custom" : current));
    setSuccess("Prompt template deleted");
  }, [refreshSavedPromptTemplates]);

  const trackStat = useCallback((type, detail = {}) => {
    setAppStats(recordAppStat(type, detail));
  }, []);

  const handleResetStats = useCallback(() => {
    setAppStats(resetAppStats());
    setSuccess("Statistics reset");
  }, []);

  const mapImportedTestCases = useCallback((cases) => (
    cases.map((tc) => ({
      ...tc,
      status: tc.status || defaultStatus,
      priority: tc.priority || "Normal",
      steps: tc.steps?.length ? [...tc.steps] : [""],
    }))
  ), [defaultStatus]);

  const importTestCases = useCallback((cases, errorMessage) => {
    if (errorMessage) {
      setError(errorMessage);
      return;
    }
    setTestCases(mapImportedTestCases(cases));
    setExpandedCase(0);
    trackStat("imported", {
      count: cases.length,
      label: `Imported ${cases.length} test case(s)`,
      issueKey,
    });
    setSuccess(`Imported ${cases.length} test case(s). Review them before publishing.`);
  }, [issueKey, mapImportedTestCases, trackStat]);

  const clearMsg = () => { setError(null); setSuccess(null); };

  const navigate = (id) => {
    if (id === "publish" && !testCases.length) {
      setPage("generate");
      setError("Generate or import test cases before opening Publish & Link.");
      setSuccess(null);
      return;
    }
    setPage(id);
    clearMsg();
  };

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
    trackStat("story-fetched", { label: `Fetched ${issueKey}`, issueKey });
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

  const fetchCrForAi = () => run(async () => {
    const cr = await api.release.fetchCr(crKey);
    setRelease(cr);
    setSuccess(`Fetched change request ${crKey}`);
  });

  const postComment = () => run(async () => {
    await api.jira.comment(issueKey, comment);
    setSuccess("Comment posted to Jira");
    setComment("");
  });

  const postAiComment = (commentText, targetKey = issueKey) => run(async () => {
    const key = (targetKey || issueKey).trim();
    await api.jira.comment(key, commentText);
    setSuccess(`AI result posted as a Jira comment on ${key}`);
  });

  const doGenerate = (userAttachments = []) => run(async () => {
    const count = Math.max(1, Number(testCount) || 1);
    const prompt = resolveGenerationPrompt(promptType, customPrompt, savedPromptTemplates, additionalPrompt);
    const data = await api.generate({
      issueKey,
      testType,
      testCount: count,
      promptType: prompt.promptType,
      customPrompt: prompt.customPrompt,
      additionalInstructions: prompt.additionalPrompt,
      attachments: [...userAttachments, ...(story?.attachments ?? []).filter((attachment) => attachment.data && isSupportedAttachment(attachment))].slice(0, 5),
      jiraDetails: storyText || undefined,
    });
    setTestCases((data.testCases ?? []).map((tc) => ({
      ...tc,
      status: tc.status || defaultStatus,
      priority: tc.priority || "Normal",
      steps: tc.steps?.length ? [...tc.steps] : [""],
    })));
    setExpandedCase(0);
    const generatedCount = data.testCases?.length ?? 0;
    trackStat("generated", {
      count: generatedCount,
      label: `Generated ${generatedCount} ${testType} test case(s)`,
      issueKey,
    });
    setSuccess(`Generated ${generatedCount} ${testType} test case(s). Edit any field before publishing.`);
  });

  const publishCasesAtIndices = useCallback(async (indices) => {
    if (!indices.length) return;
    if (!selectedProject) throw new Error("Select a project");
    if (!selectedFolder) throw new Error("Select a folder");

    setPublishing(true);
    setPublishMessage(null);
    clearMsg();

    setPublishProgress((current) => {
      const existing = new Map((current?.items ?? []).map((item) => [item.index, item]));
      const items = indices.map((index) => {
        const currentItem = existing.get(index);
        const testCase = testCases[index];
        return {
          index,
          name: testCase?.testName || `Test case ${index + 1}`,
          status: "pending",
          key: currentItem?.key || "",
          url: currentItem?.url || "",
          error: "",
        };
      });
      return {
        active: true,
        total: Math.max(current?.total ?? 0, testCases.length),
        items: mergePublishItems(current?.items ?? [], items),
      };
    });

    const published = [...publishedKeys];
    const publishedLinks = [...publishedTestCaseLinks];
    let successCount = 0;

    for (const index of indices) {
      const tc = testCases[index];
      if (!tc) continue;

      setPublishProgress((current) => ({
        ...current,
        items: updatePublishItem(current?.items ?? [], index, { status: "publishing", error: "" }),
      }));

      try {
        const res = await api.zephyr.publish({
          testCase: tc,
          projectId: selectedProject,
          folderId: selectedFolder,
          owner,
          statusId: "",
        });

        if (res.success === false || !res.testCaseKey) {
          throw new Error(res.error || "Publish failed");
        }

        if (!published.includes(res.testCaseKey)) {
          published.push(res.testCaseKey);
          publishedLinks.push({ key: res.testCaseKey, url: res.testCaseUrl });
        }

        successCount += 1;
        setPublishProgress((current) => ({
          ...current,
          items: updatePublishItem(current?.items ?? [], index, {
            status: "success",
            key: res.testCaseKey,
            url: res.testCaseUrl,
            error: "",
          }),
        }));
      } catch (e) {
        setPublishProgress((current) => ({
          ...current,
          items: updatePublishItem(current?.items ?? [], index, {
            status: "error",
            error: e.message || "Publish failed",
          }),
        }));
      }
    }

    setPublishedKeys(published);
    setPublishedTestCaseLinks(publishedLinks);
    setLinkIssueKeys((current) => current || issueKey);

    const failedCount = indices.length - successCount;
    if (successCount > 0) {
      trackStat("published", {
        count: successCount,
        label: `Published ${successCount} test case(s)`,
        issueKey,
      });
    }

    if (failedCount > 0 && successCount > 0) {
      setPublishMessage({
        type: "error",
        text: `Published ${successCount} test case(s); ${failedCount} failed. Retry the failed cases below.`,
      });
      setError(`Published ${successCount} test case(s); ${failedCount} failed.`);
    } else if (failedCount > 0) {
      setPublishMessage({ type: "error", text: "All publishes failed. Review the errors and retry." });
      setError("All publishes failed. Review the errors and retry.");
    } else {
      setPublishMessage({
        type: "success",
        text: `Published ${successCount} test case(s): ${published.join(", ")}.`,
      });
      setSuccess(`Published ${successCount} test case(s): ${published.join(", ")}. Link them to stories next.`);
    }

    setPublishing(false);
  }, [
    issueKey,
    owner,
    publishedKeys,
    publishedTestCaseLinks,
    selectedFolder,
    selectedProject,
    testCases,
    trackStat,
  ]);

  const publishAll = async () => {
    if (!testCases.length) {
      setError("Generate or import test cases first");
      return;
    }
    try {
      await publishCasesAtIndices(testCases.map((_, index) => index));
    } catch (e) {
      setPublishMessage({ type: "error", text: e.message });
      setError(e.message);
      setPublishing(false);
    }
  };

  const retryFailedPublishes = async () => {
    const failedIndices = (publishProgress?.items ?? [])
      .filter((item) => item.status === "error")
      .map((item) => item.index);
    if (!failedIndices.length) return;
    try {
      await publishCasesAtIndices(failedIndices);
    } catch (e) {
      setPublishMessage({ type: "error", text: e.message });
      setError(e.message);
      setPublishing(false);
    }
  };

  const linkPublished = () => run(async () => {
    if (!publishedKeys.length) throw new Error("Publish test cases first");
    const keys = linkIssueKeys.split(/[,\s]+/).map((k) => k.trim()).filter(Boolean);
    if (!keys.length) throw new Error("Enter at least one Jira story or change-ticket key");
    const res = await api.zephyr.linkBulk(publishedKeys, keys);
    if (res.errors?.length && !res.linked?.length) throw new Error(res.errors.join("; "));
    if (res.errors?.length) setLinkMessage({ type: "error", text: `Some links need attention: ${res.errors.join("; ")}` });
    else setLinkMessage({ type: "success", text: `Linked ${res.linked?.length ?? 0} new link(s); ${res.alreadyLinked?.length ?? 0} were already linked.` });
    trackStat("linked", {
      count: res.linked?.length ?? 0,
      label: `Linked ${res.linked?.length ?? 0} test case link(s)`,
      issueKey,
    });
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
    if (data.cyclesCreated) {
      trackStat("cycles-created", {
        count: data.cyclesCreated,
        label: `Created ${data.cyclesCreated} test cycle(s)`,
        issueKey: crKey,
      });
    }
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
      trackStat("ai-run", { label: "Story AI review", issueKey });
      setSuccess(data.mockMode ? "AI review (mock mode)" : "AI review complete");
      return;
    }
    const data = await api.release.aiAnalysis({ crKey: crKey || issueKey, action: actionId });
    setReleaseAi(data);
    trackStat("ai-run", { label: "Release AI analysis", issueKey: crKey || issueKey });
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
    additionalPrompt, setAdditionalPrompt,
    aiLoading, setAiLoading,
    savedPromptTemplates,
    saveCurrentPromptTemplate,
    removePromptTemplate,
    appStats,
    resetStats: handleResetStats,
    importTestCases,
    publishing,
    publishProgress,
    retryFailedPublishes,
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
    fetchStory, fetchForAi, fetchCrForAi, postComment, postAiComment, doGenerate,
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
