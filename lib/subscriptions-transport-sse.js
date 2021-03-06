import {print} from 'graphql/language/printer'

const isString = (v) => (typeof v === "string" || v instanceof String)
const isObject = (v) => (typeof v === "object" && v !== null)

export class SubscriptionClient {
  subscriptions = {}

  constructor(url, httpOptions) {
    const {timeout, getToken} = httpOptions
    this.url = url
    this.httpTimeout = timeout
    this.getToken = getToken
  }

  subscribe(options, handler) {
    const { query, variables, operationName, context } = options
    if (!query) throw new Error('Must provide `query` to subscribe.')
    if (!handler) throw new Error('Must provide `handler` to subscribe.')
    if (!isString(query) || (operationName && !isString(operationName)) || (variables && !isObject(variables)))
      throw new Error('Incorrect option types to subscribe. `subscription` must be a string, `operationName` must be a string, and `variables` must be an object.')

    let headers = {'Content-Type': 'application/json'}
    const token = this.getToken();
    if(!!token) headers.authorization = `Bearer ${token}`;

    return fetch(this.url, {
      method: 'POST',
      headers,
      body: JSON.stringify(options),
      timeout: this.httpTimeout || 1000,
    })
      .then(res => res.json())
      .then(data => {
        const subId = data.subId
        const evtSource = new EventSource(`${this.url}/${subId}`)
        this.subscriptions[subId] = {options, handler, evtSource}

        evtSource.onmessage = e => {
          const message = JSON.parse(e.data)
          this.subscriptions[subId].handler(null, message.data)
        }

        evtSource.onerror = e => {
          console.error(`EventSource connection failed for subscription ID: ${subId}. Retry.`, e)
          if(this.subscriptions[subId] && this.subscriptions[subId].evtSource) {
            this.subscriptions[subId].evtSource.close()
          }
          delete this.subscriptions[subId]
          setTimeout(() => this.subscribe(options, handler), 1000)
        }

        return subId
      })
      .catch(error => {
        console.error(`${error.message}. Subscription failed. Retry.`)
        setTimeout(() => this.subscribe(options, handler), 1000)
      })
  }

  unsubscribe(subscription) {
    subscription
      .then(subId => {
        if(this.subscriptions[subId] && this.subscriptions[subId].evtSource) {
          this.subscriptions[subId].evtSource.close()
        }
        delete this.subscriptions[subId]
      })
  }

  unsubscribeAll() {
    Object.keys(this.subscriptions).forEach(subId => {
      this.unsubscribe(parseInt(subId))
    })
  }
}

export function addGraphQLSubscriptions(networkInterface, spdyClient) {
  return Object.assign(networkInterface, {
    subscribe(request, handler) {
      return spdyClient.subscribe({
        query: print(request.query),
        variables: request.variables,
      }, handler)
    },
    unsubscribe(id) {
      spdyClient.unsubscribe(id)
    },
  })
}