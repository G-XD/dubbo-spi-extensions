/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.rmi;

import org.aopalliance.intercept.MethodInvocation;

/**
 * This class is deprecated, please use {@link org.apache.dubbo.rpc.protocol.rmi.RmiRemoteInvocation}.
 */
@Deprecated
public class RmiRemoteInvocation extends org.apache.dubbo.rpc.protocol.rmi.RmiRemoteInvocation {
    private static final long serialVersionUID = 1L;

    /**
     * executed on consumer side
     *
     * @param methodInvocation
     */
    public RmiRemoteInvocation(MethodInvocation methodInvocation) {
        super(methodInvocation);
    }
}
