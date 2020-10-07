# Moniker

[![Build Status](https://api.travis-ci.org/spinnaker/moniker.svg?branch=master)](https://travis-ci.org/spinnaker/moniker)

This project is a Frigga replacement aimed at making naming strategies within
Spinnaker more flexible.

## How it works

Every resource is assigned a Moniker which exposes `app`, `stack`, `detail`,
and `cluster` fields. If, for example, a Moniker is assigned using Frigga,
we can say that `cluster` = `app`-`stack`-`detail`. However, Frigga isn't 
required.

Every cloud provider, Spinnaker account, or even resource can decide how
Monikers get applied/derived. What this means is that we can support either
deriving and assigning a Moniker directly via the resource's name (what
Frigga does), or store the Moniker in the resource's metadata. As an example,
the new Kubernetes provider stores Monikers in annotations:

```yaml
metadata:
  annotations:
    "moniker.spinnaker.io/application": my-application-name
    "moniker.spinnaker.io/cluster": canary-frontend
  name: frontend-test-cache
```

Notice that the resource's name, cluster and application are completely
decoupled.

The logic for assigning & deriving Monikers only needs to happen in the
relevant Clouddriver integration; the rest of Spinnaker only sees the Moniker
that was assigned to a resource. This gives the rest of Spinnaker one
vocabulary for looking up resources by their relationships (in particular 
deciding which application and cluster a resource belongs to).
