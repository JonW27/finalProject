/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package opennlp.tools.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import opennlp.tools.chunker.ChunkerContextGenerator;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.ml.model.Event;
import opennlp.tools.parser.chunking.Parser;
import opennlp.tools.postag.DefaultPOSContextGenerator;
import opennlp.tools.postag.POSContextGenerator;
import opennlp.tools.util.ObjectStream;

/**
 * Abstract class extended by parser event streams which perform tagging and chunking.
 */
public abstract class AbstractParserEventStream extends opennlp.tools.util.AbstractEventStream<Parse> {

  private ChunkerContextGenerator chunkerContextGenerator;
  private POSContextGenerator tagContextGenerator;
  protected HeadRules rules;
  protected Set<String> punctSet;

  /**
   * The type of events being generated by this event stream.
   */
  protected ParserEventTypeEnum etype;
  protected boolean fixPossesives;
  protected Dictionary dict;

  public AbstractParserEventStream(ObjectStream<Parse> d, HeadRules rules, ParserEventTypeEnum etype, Dictionary dict) {
    super(d);
    this.dict = dict;
    if (etype == ParserEventTypeEnum.CHUNK) {
      this.chunkerContextGenerator = new ChunkContextGenerator();
    }
    else if (etype == ParserEventTypeEnum.TAG) {
      this.tagContextGenerator = new DefaultPOSContextGenerator(null);
    }
    this.rules = rules;
    punctSet = rules.getPunctuationTags();
    this.etype = etype;

    init();
  }

  @Override
  protected Iterator<Event> createEvents(Parse sample) {
    List<Event> newEvents = new ArrayList<>();

    Parse.pruneParse(sample);
    if (fixPossesives) {
      Parse.fixPossesives(sample);
    }
    sample.updateHeads(rules);
    Parse[] chunks = getInitialChunks(sample);
    if (etype == ParserEventTypeEnum.TAG) {
      addTagEvents(newEvents, chunks);
    }
    else if (etype == ParserEventTypeEnum.CHUNK) {
      addChunkEvents(newEvents, chunks);
    }
    else {
      addParseEvents(newEvents, Parser.collapsePunctuation(chunks,punctSet));
    }

    return newEvents.iterator();
  }

  protected void init() {
    fixPossesives = false;
  }

  public AbstractParserEventStream(ObjectStream<Parse> d, HeadRules rules, ParserEventTypeEnum etype) {
    this(d,rules,etype,null);
  }

  public static Parse[] getInitialChunks(Parse p) {
    List<Parse> chunks = new ArrayList<>();
    getInitialChunks(p, chunks);
    return chunks.toArray(new Parse[chunks.size()]);
  }

  private static void getInitialChunks(Parse p, List<Parse> ichunks) {
    if (p.isPosTag()) {
      ichunks.add(p);
    }
    else {
      Parse[] kids = p.getChildren();
      boolean allKidsAreTags = true;
      for (int ci = 0, cl = kids.length; ci < cl; ci++) {
        if (!kids[ci].isPosTag()) {
          allKidsAreTags = false;
          break;
        }
      }
      if (allKidsAreTags) {
        ichunks.add(p);
      }
      else {
        for (int ci = 0, cl = kids.length; ci < cl; ci++) {
          getInitialChunks(kids[ci], ichunks);
        }
      }
    }
  }

  /**
   * Produces all events for the specified sentence chunks
   * and adds them to the specified list.
   * @param newEvents A list of events to be added to.
   * @param chunks Pre-chunked constituents of a sentence.
   */
  protected abstract void addParseEvents(List<Event> newEvents, Parse[] chunks);

  private void addChunkEvents(List<Event> chunkEvents, Parse[] chunks) {
    List<String> toks = new ArrayList<>();
    List<String> tags = new ArrayList<>();
    List<String> preds = new ArrayList<>();
    for (int ci = 0, cl = chunks.length; ci < cl; ci++) {
      Parse c = chunks[ci];
      if (c.isPosTag()) {
        toks.add(c.getCoveredText());
        tags.add(c.getType());
        preds.add(Parser.OTHER);
      }
      else {
        boolean start = true;
        String ctype = c.getType();
        Parse[] kids = c.getChildren();
        for (int ti=0,tl=kids.length;ti<tl;ti++) {
          Parse tok = kids[ti];
          toks.add(tok.getCoveredText());
          tags.add(tok.getType());
          if (start) {
            preds.add(Parser.START + ctype);
            start = false;
          }
          else {
            preds.add(Parser.CONT + ctype);
          }
        }
      }
    }
    for (int ti = 0, tl = toks.size(); ti < tl; ti++) {
      chunkEvents.add(new Event(preds.get(ti), chunkerContextGenerator.getContext(ti, toks.toArray(new String[toks.size()]), tags.toArray(new String[tags.size()]), preds.toArray(new String[preds.size()]))));
    }
  }

  private void addTagEvents(List<Event> tagEvents, Parse[] chunks) {
    List<String> toks = new ArrayList<>();
    List<String> preds = new ArrayList<>();
    for (int ci = 0, cl = chunks.length; ci < cl; ci++) {
      Parse c = chunks[ci];
      if (c.isPosTag()) {
        toks.add(c.getCoveredText());
        preds.add(c.getType());
      }
      else {
        Parse[] kids = c.getChildren();
        for (int ti=0,tl=kids.length;ti<tl;ti++) {
          Parse tok = kids[ti];
          toks.add(tok.getCoveredText());
          preds.add(tok.getType());
        }
      }
    }
    for (int ti = 0, tl = toks.size(); ti < tl; ti++) {
      tagEvents.add(new Event(preds.get(ti), tagContextGenerator.getContext(ti,
          toks.toArray(new String[toks.size()]), preds.toArray(new String[preds.size()]), null)));
    }
  }

  /**
   * Returns true if the specified child is the last child of the specified parent.
   * @param child The child parse.
   * @param parent The parent parse.
   * @return true if the specified child is the last child of the specified parent; false otherwise.
   */
  protected boolean lastChild(Parse child, Parse parent) {
    Parse[] kids = AbstractBottomUpParser.collapsePunctuation(parent.getChildren(),punctSet);
    return kids[kids.length - 1] == child;
  }

}
