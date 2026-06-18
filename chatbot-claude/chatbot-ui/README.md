# chatbot-ui

React frontend for the streaming chatbot — a single-page chat UI that streams the
assistant's reply token-by-token and renders it as formatted Markdown. It's
domain-agnostic; the branding, suggestions, and copy in this repo are configured for the
**TaxInfoBot** demo and can be swapped without touching the streaming logic.

## Stack

- React 19 + TypeScript
- Vite 8
- [`react-markdown`](https://github.com/remarkjs/react-markdown) + `remark-gfm` for
  assistant Markdown (headings, tables, lists, code, blockquotes)
- Playwright for end-to-end tests

## Features

- **Streaming** — reads the backend's Server-Sent Events and appends each token as it
  arrives ([`src/api.ts`](src/api.ts)).
- **Conversation memory** — adopts the server-issued `X-Conversation-Id` and reuses it
  across turns so the backend can thread history.
- **Markdown rendering** — assistant messages render with GitHub-flavored Markdown;
  user messages stay plain text.
- **Welcome state** with suggestion chips and an animated "thinking" indicator.

## Run

The UI proxies `/api` to the backend at `http://localhost:8080`
(see [`vite.config.ts`](vite.config.ts)), so start [`chatbot-api`](../chatbot-api) first.

```bash
npm install
npm run dev      # http://localhost:5173
```

## Scripts

```bash
npm run build    # type-check (tsc -b) and production build
npm run lint     # eslint
npm run test:e2e # Playwright end-to-end tests (mock the API, no real calls)
```

## Disclaimer

Demo for testing and educational purposes only — not for commercial use, and not
professional tax advice. The UI also shows this disclaimer in its footer.
