# The Artisan Market — Full-Stack Build Plan
### Java 17 + Spring Boot · Next.js 14 + TypeScript · PostgreSQL · Redis · Docker

---

## Table of Contents
1. [Monorepo Structure](#monorepo-structure)
2. [IDE Setup](#ide-setup)
3. [Git Setup](#git-setup)
4. [How Frontend and Backend Connect](#how-frontend-and-backend-connect)
5. [Running Locally](#running-locally-dev-mode)
6. [Docker — Full Stack Containerisation](#docker--full-stack-containerisation)
7. [Fake Store API Seeder](#fake-store-api-seeder)
8. [Day-by-Day Plan](#day-by-day-plan-full-stack)
9. [README Structure](#readme-structure)
10. [Priority Map](#priority-map--what-to-protect-vs-what-to-cut)

---

## Monorepo Structure

One GitHub repo, two completely independent applications that communicate over HTTP.
```
artisan-market/                       <- one GitHub repo
├── backend/                          <- Spring Boot (open in IntelliJ)
│   ├── src/
│   │   └── main/java/com/artisanmarket/
│   │       ├── user/
│   │       ├── product/
│   │       ├── order/
│   │       ├── cart/
│   │       ├── payment/
│   │       ├── ai/
│   │       ├── chatbot/
│   │       └── shared/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                         <- Next.js + TypeScript (open in VS Code)
│   ├── src/
│   │   ├── app/
│   │   │   ├── page.tsx              <- / (homepage)
│   │   │   ├── layout.tsx            <- root layout (navbar + chatbot widget)
│   │   │   ├── products/
│   │   │   │   ├── page.tsx          <- /products
│   │   │   │   └── [id]/page.tsx     <- /products/123
│   │   │   ├── cart/page.tsx
│   │   │   ├── orders/page.tsx
│   │   │   ├── login/page.tsx
│   │   │   ├── register/page.tsx
│   │   │   └── seller/dashboard/page.tsx
│   │   ├── components/
│   │   │   ├── ui/
│   │   │   ├── layout/
│   │   │   ├── product/
│   │   │   ├── cart/
│   │   │   └── chatbot/
│   │   ├── lib/
│   │   │   ├── api/
│   │   │   │   ├── client.ts         <- base axios wrapper (attaches JWT)
│   │   │   │   ├── auth.api.ts
│   │   │   │   ├── products.api.ts
│   │   │   │   ├── orders.api.ts
│   │   │   │   └── cart.api.ts
│   │   │   └── hooks/
│   │   │       ├── useAuth.ts
│   │   │       └── useCart.ts
│   │   ├── types/
│   │   │   ├── product.ts
│   │   │   ├── order.ts
│   │   │   └── user.ts
│   │   └── context/
│   │       ├── AuthContext.tsx
│   │       └── CartContext.tsx
│   ├── package.json
│   ├── next.config.ts
│   └── Dockerfile
├── docs/
│   └── artisan-market.postman_collection.json
├── docker-compose.yml
├── docker-compose.dev.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## IDE Setup

Use two IDEs simultaneously — this is the standard professional workflow.

### IntelliJ IDEA → Backend only

Open IntelliJ, go to **File → Open**, and select the `backend/` folder specifically — not the monorepo root. IntelliJ auto-detects `pom.xml` and configures Maven. Run the app with the green Run button or `Shift+F10`.

### VS Code → Frontend + root files

Open VS Code at the **root** `artisan-market/` folder.

Essential VS Code extensions:
- **ESLint** + **Prettier**
- **Tailwind CSS IntelliSense**
- **Thunder Client** — lightweight API testing inside VS Code

---

## Git Setup
```bash
# 1. Create root folder and initialise
mkdir artisan-market && cd artisan-market
git init

# 2. Generate backend — download from start.spring.io, then:
mkdir backend
unzip ~/Downloads/demo.zip -d backend/

# 3. Generate frontend
npx create-next-app@latest frontend \
  --typescript --tailwind --eslint --app --src-dir \
  --import-alias "@/*"
```

### Root `.gitignore`

```gitignore
# Java / Maven
backend/target/
backend/.mvn/

# IntelliJ
../.idea/
*.iml
*.iws
*.ipr

# Node / Next.js
frontend/node_modules/
frontend/.next/
frontend/out/

# Environment files (NEVER commit these)
.env
.env.local
.env.production
*.env

# OS
.DS_Store
Thumbs.db
```

### First commit and push to GitHub
```bash
git add .
git commit -m "chore: monorepo scaffold — Spring Boot backend + Next.js frontend"

# Create empty repo on GitHub first (no README, no .gitignore), then:
git remote add origin https://github.com/yourusername/artisan-market.git
git branch -M main
git push -u origin main

# Work on a dev branch daily
git checkout -b dev
```

---

## How Frontend and Backend Connect

They are two separate processes that communicate over HTTP. That is the entire connection.
```
Browser (Next.js :3000) → HTTP via axios → Spring Boot (:8080) → JDBC → PostgreSQL + Redis
```

### The one backend change required — CORS config
```java
// shared/config/WebConfig.java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(
                "http://localhost:3000",
                "https://your-frontend.vercel.app"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

### Frontend base API client — `lib/api/client.ts`
```typescript
import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default apiClient;
```

### `frontend/.env.local` (gitignored — never committed)
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## Running Locally (Dev Mode)

### Prerequisites

- Docker Desktop
- Java 17+ (`java -version`)
- Node.js 18+ (`node -version`)

### Every time you work on the project, open three terminals:

**Terminal 1 — infrastructure:**
```bash
docker-compose -f docker-compose.dev.yml up -d
```

**Terminal 2 — backend:**
```bash
cd backend && ./mvnw spring-boot:run
# Or use IntelliJ's Run button
```

**Terminal 3 — frontend:**
```bash
cd frontend && npm run dev
```

| Service | URL |
|---------|-----|
| The app | http://localhost:3000 |
| Spring Boot API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

Open **http://localhost:3000** in your browser. That's the app. Port 8080 is only opened directly for Swagger.

---

## Docker — Full Stack Containerisation

### `docker-compose.dev.yml` — infrastructure only, for daily development
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    container_name: artisan-postgres
    environment:
      POSTGRES_DB: artisan_market
      POSTGRES_USER: artisan
      POSTGRES_PASSWORD: artisan123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U artisan -d artisan_market"]
      interval: 10s
      timeout: 5s
      retries: 5
  redis:
    image: redis:7-alpine
    container_name: artisan-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
volumes:
  postgres_data:
  redis_data:
```

### `docker-compose.yml` — full production stack
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
      interval: 10s
      retries: 5
    networks: [artisan]

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    networks: [artisan]

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET}
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      STRIPE_SECRET_KEY: ${STRIPE_SECRET_KEY}
    depends_on:
      postgres:
        condition: service_healthy
    networks: [artisan]

  frontend:
    build:
      context: ./frontend
      args:
        NEXT_PUBLIC_API_URL: http://backend:8080
    ports:
      - "3000:3000"
    depends_on: [backend]
    networks: [artisan]

networks:
  artisan:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
```

### `backend/Dockerfile`
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### `frontend/Dockerfile`
```dockerfile
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:20-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/public ./public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
EXPOSE 3000
CMD ["node", "server.js"]
```

Add to **`frontend/next.config.ts`**:
```typescript
const nextConfig = { output: 'standalone' };
export default nextConfig;
```

### `.env.example`
```env
POSTGRES_DB=artisan_market
POSTGRES_USER=artisan
POSTGRES_PASSWORD=your_password_here
JWT_SECRET=your-very-long-256-bit-secret-key-change-this
GEMINI_API_KEY=
STRIPE_SECRET_KEY=sk_test_...
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### Commands
```bash
cp .env.example .env
docker-compose up --build          # build and start everything
docker-compose up --build -d       # background mode
docker-compose logs -f backend     # watch backend logs
docker-compose logs -f frontend    # watch frontend logs
docker-compose down                # stop (keep data)
docker-compose down -v             # stop + wipe all data
```

---

## Fake Store API Seeder
```java
// product/ProductSeeder.java
@Component @RequiredArgsConstructor @Slf4j
public class ProductSeeder implements ApplicationRunner {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) return;
        log.info("Seeding from Fake Store API...");

        User seller = userRepository.save(User.builder()
            .email("seed@artisanmarket.com").fullName("The Artisan Market")
            .passwordHash("n/a").role(UserRole.SELLER).build());

        FakeProduct[] products = restTemplate.getForObject(
            "https://fakestoreapi.com/products", FakeProduct[].class);
        if (products == null) return;

        Arrays.stream(products).forEach(p ->
            productRepository.save(Product.builder()
                .title(p.title()).description(p.description())
                .price(BigDecimal.valueOf(p.price()))
                .imageUrls(List.of(p.image()))
                .category(mapCategory(p.category()))
                .stockQuantity(ThreadLocalRandom.current().nextInt(5, 50))
                .seller(seller).isActive(true).build()));

        log.info("Seeded {} products", products.length);
    }

    private ProductCategory mapCategory(String c) {
        return switch (c.toLowerCase()) {
            case "jewelery"    -> ProductCategory.HANDMADE;
            case "electronics" -> ProductCategory.VINTAGE;
            default            -> ProductCategory.ART;
        };
    }

    record FakeProduct(String title, String description,
                       double price, String image, String category) {}
}
```

Also register a `RestTemplate` bean in any `@Configuration` class:
```java
@Bean
public RestTemplate restTemplate() { return new RestTemplate(); }
```

---

## Day-by-Day Plan (Full Stack)

| Day | Backend | Frontend | End state |
|-----|---------|----------|-----------|
| 1 | JWT auth, CORS | Scaffold, auth context, login/register, navbar | Register + login works in browser |
| 2 | Product CRUD, Redis cache, seeder | Homepage, /products grid, /products/[id] | Real products visible |
| 3 | Redis cart, order placement, event listener | Cart drawer, /cart, /orders | Full purchase flow works |
| 4 | Stripe intent, verified reviews | Review form, seller dashboard | Sellers manage listings |
| 5 | Gemini enhancer + insights | AI enhance button, insights card | AI tools in dashboard |
| 6 | Chatbot intent router + Gemini fallback | Floating chatbot widget | Chatbot on every page |
| 7 | Exception handler, validation, Swagger | Skeletons, toasts, responsive | Docker full stack runs |

---

### Day 1 — Foundation: Auth

**Frontend installs:**
```bash
npm install axios @tanstack/react-query zustand react-hot-toast
```

**End of day:** Register, log in, see your name in the navbar.

**Commits:**
```
feat(backend): JWT auth, CORS config, user registration
feat(frontend): auth context, login/register pages, navbar
```

---

### Day 2 — Products

**Key React Query pattern:**
```typescript
'use client';
import { useQuery } from '@tanstack/react-query';
import { getProducts } from '@/lib/api/products.api';

export default function ProductsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['products'],
    queryFn: () => getProducts(),
  });
  if (isLoading) return <ProductGridSkeleton />;
  return <ProductGrid products={data?.content ?? []} />;
}
```

**End of day:** Seeded products visible in the browser.

**Commits:**
```
feat(backend): product domain, Redis cache, Fake Store API seeder
feat(frontend): homepage, product browse, product detail
```

---

### Day 3 — Cart & Orders

**End of day:** Browse → add to cart → place order → order history.

**Commits:**
```
feat(backend): Redis cart, order placement, event-driven notification stub
feat(frontend): cart drawer, cart page, orders flow
```

---

### Day 4 — Payments & Reviews

**End of day:** Sellers manage listings, buyers leave verified reviews.

**Commits:**
```
feat(backend): Stripe payment intent, verified purchase reviews
feat(frontend): reviews UI, seller dashboard
```

---

### Day 5 — AI Insights

**End of day:** AI tools visible and working in the seller dashboard.

**Commits:**
```
feat(backend): Gemini description enhancer + product insights
feat(frontend): AI enhance button, insights card in seller dashboard
```

---

### Day 6 — Chatbot

**Floating widget skeleton:**
```typescript
// components/chatbot/ChatbotWidget.tsx
'use client';
export default function ChatbotWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');

  const sendMessage = async () => {
    setMessages(prev => [...prev, { role: 'user', content: input }]);
    setInput('');
    const { data } = await chatbotApi.sendMessage(input);
    setMessages(prev => [...prev, { role: 'assistant', content: data.reply }]);
  };

  return (
    <>
      <button onClick={() => setOpen(!open)}
        className="fixed bottom-6 right-6 bg-amber-600 text-white rounded-full p-4 shadow-lg z-50">
        💬
      </button>
      {open && <ChatPanel messages={messages} onSend={sendMessage}
                          input={input} setInput={setInput} />}
    </>
  );
}
```

**End of day:** Chatbot bubble on every page, working end-to-end.

**Commits:**
```
feat(backend): chatbot with Gemini fallback, Redis session
feat(frontend): floating chatbot widget
```

---

### Day 7 — Polish, Docker, README

**Backend:** `GlobalExceptionHandler`, `@Valid` on all request bodies, pagination, Swagger.

**Frontend:** Loading skeletons, error states, `react-hot-toast` notifications, responsive polish.

**Docker:** Run `docker-compose up --build` and verify the full stack works containerised.

**Commits:**
```
feat(backend): global exception handling, validation, Swagger docs
feat(frontend): loading states, toast notifications, responsive
chore: full Docker stack verified, README complete
```

---

## README Structure

The most important sections in order:

1. **One-line description + screenshot/gif** — first impression
2. **Tech stack table** — scannable in 10 seconds
3. **Architecture paragraph** — "why modular monolith" — this separates you from tutorial-followers
4. **Key technical decisions** — explain the *why* (Redis for cart, event-driven side effects, verified reviews, Gemini context injection)
5. **Running locally** — must work first try, two options (Docker one-liner + dev mode)
6. **API docs link** — Swagger or Postman collection

---

## Priority Map — What to Protect vs What to Cut

### Never cut
- JWT auth + login/register pages
- Product browse (grid + detail) — what a recruiter will click
- Cart + order flow — it is an e-commerce site
- `docker-compose.dev.yml` (Postgres + Redis)
- README with architecture explanation

### High value — do if you can
- Full `docker-compose.yml` one-command stack
- Chatbot widget — visually memorable
- Seller dashboard — demonstrates RBAC
- Swagger UI — professionalism signal

### Cut cleanly if behind
- **Stripe** — mock it, document as stubbed. Half-finished is worse.
- **AI description enhancer** — self-contained, skipping breaks nothing
- **AI seller insights** — same
- **Reviews** — worth having, not a blocker

### What actually impresses a senior engineer

1. Domain-based package structure — they will look at your `src/` tree
2. README that explains the *why* not just the *what*
3. `docker-compose up --build` that actually works
4. Swagger UI
5. Chatbot with Gemini context injection — memorable

The difference between a junior and mid-level portfolio project is not feature count — it is intentionality and how clearly decisions are communicated.

---

*Once the project is live, move this file to `docs/PLAN.md`. Don't leave a checklist as your GitHub landing page.*