package org.renjin.ci.workflow.queue;


public enum NodeState  {
  READY,
  BUILT,
  BLOCKED,
  LEASED;
}
